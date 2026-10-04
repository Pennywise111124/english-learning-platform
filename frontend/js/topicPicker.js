// topicPicker.js: ô chọn Topic có tìm kiếm (combobox) dùng chung cho Admin và trang Dictation.
// Gõ từ khoá -> gọi server (GET /api/topics) lấy tối đa 8 kết quả, nên không bị giới hạn 100 Topic.
// Bàn phím: ↑ ↓ chọn, Enter xác nhận, Esc đóng. Có ARIA combobox/listbox.

import { getTopics, getTopicDetail } from './api.js';
import { el, buildLevelBadge } from './ui.js';

const RESULT_LIMIT = 8;
const DEBOUNCE_MS = 250;
let pickerSeq = 0;

/**
 * @param {Object} options
 * @param {HTMLElement} options.container  phần tử rỗng để gắn ô chọn vào
 * @param {string} [options.inputId]       id cho <input> (để <label for> trỏ tới)
 * @param {string} [options.ariaLabel]
 * @param {string} [options.placeholder]
 * @param {(topic: {id:number,title:string,level:string}|null) => void} [options.onChange]
 *        chỉ gọi khi NGƯỜI DÙNG chọn/xoá, không gọi khi setValue()/setValueById()
 */
export function createTopicPicker({ container, inputId, ariaLabel = 'Topic', placeholder = 'Search topics...', onChange = () => {} }) {
    const uid = `topic-picker-${++pickerSeq}`;

    let selected = null;        // { id, title, level } | null
    let items = [];
    let totalElements = 0;
    let state = 'idle';         // idle | loading | ready | error
    let activeIndex = -1;
    let isOpen = false;
    let typing = false;         // true từ lúc người dùng gõ; false khi mở ô hoặc vừa chọn
    let dirty = false;          // có chữ mới gõ nhưng kết quả chưa cập nhật
    let lastKeyword = '';
    let requestId = 0;
    let timer = null;
    let selectOnMouseUp = false;

    // ── DOM ──
    const root = el('div', 'relative');

    const searchIcon = el('span', 'material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant', 'search');
    searchIcon.setAttribute('aria-hidden', 'true');

    const input = el('input', 'w-full bg-surface-container-lowest border-2 border-outline-variant rounded-2xl pl-12 pr-12 py-3 font-label-md text-label-md text-on-surface placeholder:text-on-surface-variant placeholder:font-normal transition-colors hover:border-primary focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/20');
    input.type = 'text';
    input.placeholder = placeholder;
    input.autocomplete = 'off';
    input.spellcheck = false;
    if (inputId) input.id = inputId;
    input.setAttribute('role', 'combobox');
    input.setAttribute('aria-autocomplete', 'list');
    input.setAttribute('aria-expanded', 'false');
    input.setAttribute('aria-controls', `${uid}-list`);
    input.setAttribute('aria-label', ariaLabel);

    const clearBtn = el('button', 'hidden absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-on-surface-variant hover:bg-surface-container-high transition-colors');
    clearBtn.type = 'button';
    clearBtn.setAttribute('aria-label', 'Clear topic');
    clearBtn.append(el('span', 'material-symbols-outlined text-lg', 'close'));

    const panel = el('div', 'hidden absolute z-30 left-0 right-0 mt-2 bg-surface-container-lowest border-2 border-outline-variant rounded-2xl shadow-lg overflow-hidden');
    const list = el('ul', 'max-h-72 overflow-y-auto py-1 transition-opacity');
    list.id = `${uid}-list`;
    list.setAttribute('role', 'listbox');
    list.setAttribute('aria-label', ariaLabel);
    const message = el('div', 'hidden px-4 py-3 text-center text-on-surface-variant');
    const footer = el('p', 'hidden px-4 py-2 border-t-2 border-outline-variant font-body-sm text-body-sm text-on-surface-variant');
    const retryBtn = el('button', 'mt-2 text-primary font-label-md text-label-md hover:underline', 'Try again');
    retryBtn.type = 'button';
    const live = el('span', 'sr-only');
    live.setAttribute('role', 'status');

    panel.append(list, message, footer);
    root.append(searchIcon, input, clearBtn, panel, live);
    container.replaceChildren(root);

    // ── Hiển thị ──

    function updateClear() {
        clearBtn.classList.toggle('hidden', !(selected || input.value));
    }

    function revertText() {
        typing = false;
        input.value = selected ? selected.title : '';
        updateClear();
    }

    function setActive(index, scroll = false) {
        const options = list.children;
        if (activeIndex >= 0 && options[activeIndex]) options[activeIndex].classList.remove('bg-surface-container-high');
        activeIndex = index;
        const option = index >= 0 ? options[index] : null;
        if (option) {
            option.classList.add('bg-surface-container-high');
            input.setAttribute('aria-activedescendant', option.id);
            if (scroll) option.scrollIntoView({ block: 'nearest' });
        } else {
            activeIndex = -1;
            input.removeAttribute('aria-activedescendant');
        }
    }

    function buildOption(topic, index) {
        const isSelected = !!selected && selected.id === topic.id;
        const li = el('li', 'mx-1 my-0.5 px-3 py-2 rounded-xl cursor-pointer flex items-center justify-between gap-3 transition-colors');
        li.id = `${uid}-opt-${index}`;
        li.setAttribute('role', 'option');
        li.setAttribute('aria-selected', String(isSelected));

        const titleWrap = el('span', 'flex items-center gap-2 min-w-0');
        if (isSelected) {
            const check = el('span', 'material-symbols-outlined text-primary', 'check');
            check.style.fontSize = '20px';
            check.setAttribute('aria-hidden', 'true');
            titleWrap.append(check);
        }
        titleWrap.append(el('span', `font-body-md text-body-md text-on-surface break-words min-w-0 ${isSelected ? 'font-bold' : ''}`, topic.title));

        li.append(titleWrap, buildLevelBadge(topic.level));
        li.addEventListener('mouseenter', () => setActive(index));
        li.addEventListener('click', () => choose(topic));
        return li;
    }

    function render() {
        activeIndex = -1;
        input.removeAttribute('aria-activedescendant');
        list.replaceChildren();
        message.replaceChildren();
        message.classList.add('hidden');
        list.classList.remove('hidden', 'opacity-60');
        footer.classList.add('hidden');

        if (state === 'loading' && items.length === 0) {
            list.classList.add('hidden');
            message.classList.remove('hidden');
            message.append(...Array.from({ length: 3 }, () => el('div', 'skeleton h-9 rounded-xl my-2')));
            return;
        }
        if (state === 'error') {
            list.classList.add('hidden');
            message.classList.remove('hidden');
            message.append(el('p', 'font-body-md text-body-md', 'Could not load topics.'), retryBtn);
            return;
        }
        if (state === 'ready' && items.length === 0) {
            list.classList.add('hidden');
            message.classList.remove('hidden');
            message.append(el('p', 'font-body-md text-body-md', lastKeyword ? `No topics match “${lastKeyword}”` : 'No topics yet'));
            return;
        }

        list.append(...items.map(buildOption));
        if (state === 'loading') list.classList.add('opacity-60');   // giữ kết quả cũ mờ đi trong lúc tải, tránh nháy
        if (state === 'ready' && totalElements > items.length) {
            footer.textContent = `Showing ${items.length} of ${totalElements}. Keep typing to narrow down.`;
            footer.classList.remove('hidden');
        }
    }

    // ── Mở / đóng / tìm kiếm ──

    function open() {
        if (isOpen) return;
        isOpen = true;
        panel.classList.remove('hidden');
        input.setAttribute('aria-expanded', 'true');
    }

    function close() {
        clearTimeout(timer);
        requestId++;       // bỏ qua mọi phản hồi đang chờ
        dirty = false;
        setActive(-1);
        if (!isOpen) return;
        isOpen = false;
        panel.classList.add('hidden');
        input.setAttribute('aria-expanded', 'false');
    }

    async function search() {
        clearTimeout(timer);
        dirty = false;
        const keyword = typing ? input.value.trim() : '';
        lastKeyword = keyword;
        const myId = ++requestId;
        state = 'loading';
        render();
        try {
            // Chưa gõ gì: hiện Topic mới nhất. Có từ khoá: sắp xếp theo tên.
            const data = await getTopics({ keyword, size: RESULT_LIMIT, sort: keyword ? 'title' : 'newest' });
            if (myId !== requestId) return;   // đã có yêu cầu mới hơn hoặc ô đã đóng
            items = data.content || [];
            totalElements = data.totalElements ?? items.length;
            state = 'ready';
            render();
            const selectedIndex = selected ? items.findIndex(t => t.id === selected.id) : -1;
            setActive(typing ? (items.length ? 0 : -1) : selectedIndex, true);
            live.textContent = items.length
                ? `${totalElements} topic${totalElements === 1 ? '' : 's'} found`
                : 'No topics found';
        } catch (error) {
            if (myId !== requestId) return;
            console.error('[topicPicker]', error);
            items = [];
            state = 'error';
            render();
        }
    }

    function openAndSearch() {
        typing = false;
        open();
        search();
    }

    function choose(topic) {
        selected = { id: topic.id, title: topic.title, level: topic.level };
        typing = false;
        input.value = selected.title;
        updateClear();
        close();
        onChange(selected);
    }

    // ── Sự kiện ──

    input.addEventListener('mousedown', () => { selectOnMouseUp = document.activeElement !== input; });
    input.addEventListener('mouseup', (e) => {
        if (selectOnMouseUp) { e.preventDefault(); selectOnMouseUp = false; }   // giữ vùng chọn do focus() tạo ra
    });
    input.addEventListener('focus', () => { openAndSearch(); input.select(); });
    input.addEventListener('click', () => { if (!isOpen) openAndSearch(); });
    input.addEventListener('blur', () => { revertText(); close(); });

    input.addEventListener('input', () => {
        typing = true;
        dirty = true;
        open();
        updateClear();
        clearTimeout(timer);
        timer = setTimeout(search, DEBOUNCE_MS);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (!isOpen) openAndSearch();
            else if (items.length) setActive((activeIndex + 1) % items.length, true);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (isOpen && items.length) setActive(activeIndex <= 0 ? items.length - 1 : activeIndex - 1, true);
        } else if (e.key === 'Enter') {
            if (!isOpen) return;
            e.preventDefault();
            if (dirty) { search(); return; }   // vừa gõ xong, kết quả đang cũ: tìm lại trước đã
            if (activeIndex >= 0 && items[activeIndex]) choose(items[activeIndex]);
        } else if (e.key === 'Escape' && isOpen) {
            e.preventDefault();
            e.stopPropagation();
            revertText();
            close();
        }
    });

    // Giữ focus ở ô nhập khi bấm vào panel / nút xoá (nếu không, blur sẽ đóng panel trước khi click kịp chạy)
    panel.addEventListener('mousedown', (e) => e.preventDefault());
    clearBtn.addEventListener('mousedown', (e) => e.preventDefault());
    retryBtn.addEventListener('click', () => search());

    clearBtn.addEventListener('click', () => {
        const hadSelection = selected !== null;
        selected = null;
        typing = false;
        input.value = '';
        updateClear();
        if (document.activeElement === input) openAndSearch();
        else input.focus();        // focus handler sẽ mở panel
        if (hadSelection) onChange(null);
    });

    // Trên iOS, chạm ra ngoài đôi khi không làm ô nhập mất focus
    function onDocumentPointerDown(e) {
        if (isOpen && !root.contains(e.target)) input.blur();
    }
    document.addEventListener('pointerdown', onDocumentPointerDown);

    updateClear();

    // ── API công khai ──

    function setValue(topic) {
        selected = topic ? { id: topic.id, title: topic.title, level: topic.level } : null;
        typing = false;
        input.value = selected ? selected.title : '';
        updateClear();
    }

    async function setValueById(id) {
        try {
            const topic = await getTopicDetail(id);
            setValue(topic);
            return selected;
        } catch (error) {
            console.error('[topicPicker] setValueById', error);
            return null;
        }
    }

    return {
        getValue: () => selected,
        setValue,
        setValueById,
        focus: () => input.focus(),
        destroy: () => document.removeEventListener('pointerdown', onDocumentPointerDown)
    };
}
