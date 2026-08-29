# Cách sử dụng NavBar Component

## File: `frontend/js/navbar.js`

NavBar component được tạo để tái sử dụng cho tất cả các trang sau khi login.

## Cách dùng

### Option 1: Replace existing navbar (Recommended)

```javascript
import { initNavBar } from './js/navbar.js';

// Call after checkAuth() and after DOM loads
initNavBar('chat'); // 'chat' | 'topics' | 'progress' | 'profile' | 'admin'
```

### Option 2: Inject into container

```html
<div id="navbar-container"></div>

<script type="module">
import { injectNavBar } from './js/navbar.js';
injectNavBar('navbar-container', 'topics');
</script>
```

### Option 3: Get HTML string

```javascript
import { renderNavBar } from './js/navbar.js';

const navHTML = renderNavBar('progress');
document.querySelector('header').innerHTML = navHTML;
```

## Tính năng

✅ **Tự động hiển thị link Admin** nếu user có role ADMIN  
✅ **Avatar đồng bộ** từ localStorage  
✅ **Active state** tự động dựa vào page parameter  
✅ **Responsive** và match design hiện tại  

## Ví dụ update 1 trang

**Before:**
```html
<nav class="...">
  <!-- 30+ lines of HTML -->
</nav>

<script>
  // Check admin, show link...
</script>
```

**After:**
```html
<nav id="main-nav"></nav>

<script type="module">
  import { initNavBar } from './js/navbar.js';
  import { checkAuth } from './js/auth.js';
  
  checkAuth();
  initNavBar('chat'); // Current page
</script>
```

## Next Steps

1. Update từng trang HTML để dùng navbar.js
2. Xóa inline navbar HTML
3. Xóa duplicate admin link logic
4. Avatar sẽ tự động đồng bộ từ login response
