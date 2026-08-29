# Fix NavBar Not Showing

## Vấn đề: 
NavBar không hiển thị vì script chưa import navbar.js

## Đã fix:
Thêm `import { initNavBar } from './js/navbar.js';` vào topics.html

## Test lại:

1. Vào `http://localhost:8000/topics.html`
2. Hard reload: **Ctrl + Shift + R**
3. Mở Console (F12)

Sẽ thấy logs:
```
[NavBar] Initializing with user: {...}
[NavBar] Rendered into #navbar-container
```

Nếu vẫn không thấy NavBar, check Console có error không.
