// ui.js: các hàm dựng giao diện dùng chung (không phụ thuộc trang nào)

// Tạo phần tử bằng textContent: dữ liệu từ server/người dùng KHÔNG đi qua innerHTML (chống XSS)
export function el(tag, className = '', text = '') {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
}

const LEVEL_BADGE = {
    BEGINNER:     { label: 'Beginner',     box: 'bg-secondary-fixed text-on-secondary-fixed border-secondary/50', dot: 'bg-secondary' },
    INTERMEDIATE: { label: 'Intermediate', box: 'bg-primary-fixed text-on-primary-fixed border-primary/50',       dot: 'bg-primary' },
    ADVANCED:     { label: 'Advanced',     box: 'bg-tertiary-fixed text-on-tertiary-fixed border-tertiary/50',    dot: 'bg-tertiary' }
};

export function buildLevelBadge(level) {
    const cfg = LEVEL_BADGE[level] || {
        label: level, box: 'bg-surface-container-high text-on-surface-variant border-outline-variant', dot: 'bg-on-surface-variant'
    };
    const badge = el('span', `inline-flex items-center gap-1.5 w-fit px-3 py-1 rounded-full border-2 font-label-sm text-label-sm font-bold whitespace-nowrap ${cfg.box}`);
    badge.append(el('span', `w-2 h-2 rounded-full ${cfg.dot}`), document.createTextNode(cfg.label));
    return badge;
}

// Nút phân trang: cùng style với vocabulary.html (btn-3d khi bật, nền xám khi tắt)
export function buildPagerButton(label, disabled, onClick) {
    const base = 'px-5 py-2.5 rounded-2xl font-label-md text-label-md transition-all';
    const state = disabled
        ? 'bg-surface-container text-on-surface-variant cursor-not-allowed'
        : 'btn-3d bg-primary border-primary-shadow text-on-primary hover:brightness-105';
    const btn = el('button', `${base} ${state}`, label);
    btn.type = 'button';
    btn.disabled = disabled;
    btn.addEventListener('click', onClick);
    return btn;
}

export function buildPageInfo(text) {
    return el('span', 'font-label-md text-label-md text-on-surface', text);
}

// Danh sách Level dùng cho mọi bộ lọc (chấm màu khớp với buildLevelBadge)
export const LEVEL_FILTER_OPTIONS = [
    { value: '', label: 'All levels' },
    { value: 'BEGINNER', label: 'Beginner', dot: 'bg-secondary' },
    { value: 'INTERMEDIATE', label: 'Intermediate', dot: 'bg-primary' },
    { value: 'ADVANCED', label: 'Advanced', dot: 'bg-tertiary' }
];

let selectSeq = 0;

/**
 * Dropdown tự vẽ cho bộ lọc (thay <select> gốc).
 * - options: [{ value, label, dot? }]; mục đầu tiên là giá trị mặc định (khác nó thì nút đổi viền để báo đang lọc)
 * - onChange(value): chỉ gọi khi NGƯỜI DÙNG đổi giá trị, không gọi khi setValue()
 */
export function createSelect({ container, label, options, value, icon = '', onChange = () => {} }) {
    const uid = `select-${++selectSeq}`;
    let current = options.some(o => o.value === value) ? value : options[0].value;
    let isOpen = false;
    let activeIndex = -1;

    const root = el('div', 'relative');

    const trigger = el('button', 'w-full flex items-center gap-2 bg-surface-container-lowest border-2 rounded-2xl pl-4 pr-3 py-3 font-label-md text-label-md text-on-surface text-left transition-colors hover:border-primary focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/20');
    trigger.type = 'button';
    trigger.setAttribute('role', 'combobox');
    trigger.setAttribute('aria-haspopup', 'listbox');
    trigger.setAttribute('aria-expanded', 'false');
    trigger.setAttribute('aria-controls', `${uid}-list`);
    trigger.setAttribute('aria-label', label);

    const leadIcon = icon ? el('span', 'material-symbols-outlined text-on-surface-variant', icon) : null;
    if (leadIcon) { leadIcon.style.fontSize = '20px'; leadIcon.setAttribute('aria-hidden', 'true'); }
    const dotEl = el('span', 'hidden');
    const textEl = el('span', 'flex-1 truncate');
    const chevron = el('span', 'material-symbols-outlined text-on-surface-variant transition-transform duration-150', 'expand_more');
    chevron.setAttribute('aria-hidden', 'true');
    trigger.append(...[leadIcon, dotEl, textEl, chevron].filter(Boolean));

    const panel = el('div', 'absolute z-30 left-0 right-0 mt-2 bg-surface-container-lowest border-2 border-outline-variant rounded-2xl shadow-lg overflow-hidden invisible opacity-0 -translate-y-1 transition-all duration-150');
    const list = el('ul', 'py-1 max-h-72 overflow-y-auto');
    list.id = `${uid}-list`;
    list.setAttribute('role', 'listbox');
    list.setAttribute('aria-label', label);
    panel.append(list);
    root.append(trigger, panel);
    container.replaceChildren(root);

    const optionEls = options.map((opt, index) => {
        const li = el('li', 'mx-1 my-0.5 px-3 py-2 rounded-xl cursor-pointer flex items-center gap-2 font-body-md text-body-md text-on-surface transition-colors');
        li.id = `${uid}-opt-${index}`;
        li.setAttribute('role', 'option');
        if (opt.dot) li.append(el('span', `w-2 h-2 rounded-full flex-shrink-0 ${opt.dot}`));
        li.append(el('span', 'flex-1 min-w-0 truncate', opt.label));
        const check = el('span', 'material-symbols-outlined text-primary invisible', 'check');
        check.style.fontSize = '20px';
        check.setAttribute('aria-hidden', 'true');
        li.append(check);
        li.addEventListener('mouseenter', () => setActive(index));
        li.addEventListener('click', () => choose(opt.value));
        return li;
    });
    list.append(...optionEls);

    function refresh() {
        const opt = options.find(o => o.value === current);
        textEl.textContent = opt.label;
        dotEl.className = opt.dot ? `w-2 h-2 rounded-full flex-shrink-0 ${opt.dot}` : 'hidden';
        const changed = current !== options[0].value;   // khác mặc định = đang lọc
        trigger.classList.toggle('border-primary', changed);
        trigger.classList.toggle('border-outline-variant', !changed);
        optionEls.forEach((li, i) => {
            const selected = options[i].value === current;
            li.setAttribute('aria-selected', String(selected));
            li.classList.toggle('font-bold', selected);
            li.lastElementChild.classList.toggle('invisible', !selected);
        });
    }

    function setActive(index, scroll = false) {
        if (activeIndex >= 0) optionEls[activeIndex].classList.remove('bg-surface-container-high');
        activeIndex = index;
        if (index >= 0) {
            optionEls[index].classList.add('bg-surface-container-high');
            trigger.setAttribute('aria-activedescendant', optionEls[index].id);
            if (scroll) optionEls[index].scrollIntoView({ block: 'nearest' });
        } else {
            trigger.removeAttribute('aria-activedescendant');
        }
    }

    function open() {
        if (isOpen) return;
        isOpen = true;
        panel.classList.remove('invisible', 'opacity-0', '-translate-y-1');
        chevron.classList.add('rotate-180');
        trigger.setAttribute('aria-expanded', 'true');
        setActive(options.findIndex(o => o.value === current), true);
    }

    function close() {
        if (!isOpen) return;
        isOpen = false;
        panel.classList.add('invisible', 'opacity-0', '-translate-y-1');
        chevron.classList.remove('rotate-180');
        trigger.setAttribute('aria-expanded', 'false');
        setActive(-1);
    }

    function choose(newValue) {
        const changed = newValue !== current;
        current = newValue;
        refresh();
        close();
        trigger.focus();
        if (changed) onChange(current);
    }

    trigger.addEventListener('click', () => {
        trigger.focus();   // Safari không tự focus nút khi bấm chuột, nên cần focus để dùng được bàn phím
        if (isOpen) close(); else open();
    });

    trigger.addEventListener('keydown', (e) => {
        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                if (!isOpen) open(); else setActive(Math.min(activeIndex + 1, options.length - 1), true);
                break;
            case 'ArrowUp':
                e.preventDefault();
                if (!isOpen) open(); else setActive(Math.max(activeIndex - 1, 0), true);
                break;
            case 'Home':
                if (isOpen) { e.preventDefault(); setActive(0, true); }
                break;
            case 'End':
                if (isOpen) { e.preventDefault(); setActive(options.length - 1, true); }
                break;
            case 'Enter':
            case ' ':
                e.preventDefault();   // tự xử lý để nút không bị bấm hai lần (keydown rồi click)
                if (!isOpen) open();
                else if (activeIndex >= 0) choose(options[activeIndex].value);
                break;
            case 'Escape':
                if (isOpen) { e.preventDefault(); e.stopPropagation(); close(); }
                break;
            case 'Tab':
                close();
                break;
        }
    });
    trigger.addEventListener('keyup', (e) => { if (e.key === ' ') e.preventDefault(); });

    // Giữ focus ở nút khi bấm vào panel (nếu không, trình duyệt chuyển focus đi trước khi click kịp chạy)
    panel.addEventListener('mousedown', (e) => e.preventDefault());

    function onDocumentPointerDown(e) {
        if (isOpen && !root.contains(e.target)) close();
    }
    document.addEventListener('pointerdown', onDocumentPointerDown);

    refresh();

    return {
        getValue: () => current,
        setValue: (v) => { if (options.some(o => o.value === v)) { current = v; refresh(); } },   // không gọi onChange
        destroy: () => document.removeEventListener('pointerdown', onDocumentPointerDown)
    };
}

/**
 * Thanh lọc cho danh sách đã tải sẵn trong trình duyệt:
 * ô tìm kiếm + (tuỳ chọn) lọc Level + (tuỳ chọn) công tắc + bộ đếm.
 * - getState(): { keyword (chữ thường), text (chữ gốc), level, toggle }
 * - reset(): xoá bộ lọc, KHÔNG gọi onChange (nơi gọi tự vẽ lại)
 * - setCount(shown, total): cập nhật dòng "Showing x of y"
 */
export function createFilterBar({ container, label, placeholder, noun = 'item', toggleLabel = '', levelFilter = false, onChange = () => {} }) {
    const root = el('div', 'flex flex-col gap-1 mb-stack-sm');
    const row = el('div', 'flex flex-col md:flex-row gap-3 md:items-center');

    const field = el('div', 'relative flex-1');
    const icon = el('span', 'material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant', 'search');
    icon.setAttribute('aria-hidden', 'true');
    const input = el('input', 'w-full bg-surface-container-lowest border-2 border-outline-variant rounded-2xl pl-12 pr-12 py-3 font-body-md text-body-md text-on-surface placeholder:text-on-surface-variant transition-colors hover:border-primary focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/20');
    input.type = 'text';
    input.placeholder = placeholder;
    input.autocomplete = 'off';
    input.setAttribute('aria-label', label);
    const clearBtn = el('button', 'hidden absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-on-surface-variant hover:bg-surface-container-high transition-colors');
    clearBtn.type = 'button';
    clearBtn.setAttribute('aria-label', 'Clear search');
    clearBtn.append(el('span', 'material-symbols-outlined text-lg', 'close'));
    field.append(icon, input, clearBtn);
    row.append(field);

    let levelSelect = null;
    if (levelFilter) {
        const levelWrap = el('div', 'md:w-52');
        levelSelect = createSelect({
            container: levelWrap, label: 'Filter by level', icon: 'signal_cellular_alt',
            options: LEVEL_FILTER_OPTIONS, value: '',
            onChange: () => { updateUi(); onChange(); }
        });
        row.append(levelWrap);
    }

    let toggle = null;
    if (toggleLabel) {
        const wrap = el('label', 'inline-flex items-center gap-2 cursor-pointer font-label-md text-label-md text-on-surface select-none flex-shrink-0');
        toggle = el('input', 'w-4 h-4 rounded text-primary accent-primary focus:ring-primary/30');
        toggle.type = 'checkbox';
        wrap.append(toggle, document.createTextNode(toggleLabel));
        row.append(wrap);
    }

    const meta = el('div', 'flex items-center justify-between min-h-[1.5rem]');
    const summary = el('p', 'font-label-md text-label-md text-on-surface-variant');
    summary.setAttribute('aria-live', 'polite');
    const clearAll = el('button', 'hidden font-label-md text-label-md text-primary hover:underline', 'Clear filters');
    clearAll.type = 'button';
    meta.append(summary, clearAll);

    root.append(row, meta);
    container.replaceChildren(root);

    let timer = null;
    const isActive = () => input.value.trim() !== ''
        || (levelSelect !== null && levelSelect.getValue() !== '')
        || (toggle !== null && toggle.checked);

    function updateUi() {
        clearBtn.classList.toggle('hidden', input.value === '');
        clearAll.classList.toggle('hidden', !isActive());
    }

    function reset() {
        clearTimeout(timer);
        input.value = '';
        if (levelSelect) levelSelect.setValue('');
        if (toggle) toggle.checked = false;
        updateUi();
    }

    input.addEventListener('input', () => {
        updateUi();
        clearTimeout(timer);
        timer = setTimeout(onChange, 150);   // lọc cục bộ nên chỉ cần debounce ngắn
    });
    if (toggle) toggle.addEventListener('change', () => { updateUi(); onChange(); });
    clearBtn.addEventListener('click', () => { input.value = ''; updateUi(); onChange(); input.focus(); });
    clearAll.addEventListener('click', () => { reset(); onChange(); });

    return {
        getState: () => ({
            keyword: input.value.trim().toLowerCase(),
            text: input.value.trim(),
            level: levelSelect ? levelSelect.getValue() : '',
            toggle: toggle ? toggle.checked : false
        }),
        reset,
        setVisible: (visible) => root.classList.toggle('hidden', !visible),
        setCount: (shown, total) => {
            if (total === 0) { summary.textContent = ''; return; }
            const plural = total === 1 ? '' : 's';
            summary.textContent = isActive()
                ? `Showing ${shown} of ${total} ${noun}${plural}`
                : `${total} ${noun}${plural}`;
        }
    };
}

export function createSearchBox({ container, label, placeholder, maxLength = 50, delay = 300, onSearch = () => {} }) {
    const root = el('div', 'relative');
    const icon = el('span', 'material-symbols-outlined pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant', 'search');
    icon.setAttribute('aria-hidden', 'true');
    const input = el('input', 'w-full bg-surface-container-lowest border-2 border-outline-variant rounded-2xl pl-12 pr-12 py-3 font-body-md text-body-md text-on-surface placeholder:text-on-surface-variant transition-colors hover:border-primary focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/20');
    input.type = 'text';
    input.placeholder = placeholder;
    input.maxLength = maxLength;   // khớp giới hạn 50 ký tự của backend
    input.autocomplete = 'off';
    input.setAttribute('aria-label', label);
    const clearBtn = el('button', 'hidden absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-on-surface-variant hover:bg-surface-container-high transition-colors');
    clearBtn.type = 'button';
    clearBtn.setAttribute('aria-label', 'Clear search');
    clearBtn.append(el('span', 'material-symbols-outlined text-lg', 'close'));
    root.append(icon, input, clearBtn);
    container.replaceChildren(root);

    let timer = null;
    let lastValue = '';

    const updateClear = () => clearBtn.classList.toggle('hidden', input.value === '');

    function commit() {
        clearTimeout(timer);
        const value = input.value.trim();
        if (value === lastValue) return;   // không đổi thì không gọi lại API
        lastValue = value;
        onSearch(value);
    }

    input.addEventListener('input', () => {
        updateClear();
        clearTimeout(timer);
        timer = setTimeout(commit, delay);
    });
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') { e.preventDefault(); commit(); }
    });
    clearBtn.addEventListener('click', () => {
        input.value = '';
        updateClear();
        commit();
        input.focus();
    });

    return {
        getValue: () => lastValue,
        setValue: (value) => {
            clearTimeout(timer);
            input.value = value;
            lastValue = value.trim();
            updateClear();
        },
        focus: () => input.focus()
    };
}
