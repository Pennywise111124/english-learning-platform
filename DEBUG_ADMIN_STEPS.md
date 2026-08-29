# Debug Steps - Admin Login Issue

## Bước 1: Kiểm tra debug logs

1. Mở `http://localhost:8000/test-admin.html`
2. Mở Console (F12)
3. Click button "Test Login"
4. Xem Console logs:

**Nếu thấy:**
```
[mockApi] Login attempt: {username: "admin", password: "admin123"}
[mockApi] Checking admin: true true
[mockApi] ✅ ADMIN LOGIN SUCCESS
```
→ **Code đúng**, vấn đề là browser cache

**Nếu KHÔNG thấy logs trên:**
→ Browser đang load file mockApi.js CŨ

**Nếu thấy:**
```
[mockApi] Checking admin: false true
```
→ Username có vấn đề (space, encoding)

**Nếu thấy:**
```
[mockApi] Checking admin: true false
```
→ Password có vấn đề

## Bước 2: Force reload module cache

Trong Console, chạy:
```javascript
// Clear service worker cache
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(registrations => {
    registrations.forEach(r => r.unregister());
  });
}

// Add timestamp to force reload
const timestamp = Date.now();
import(`./js/mockApi.js?v=${timestamp}`).then(module => {
  console.log('Module reloaded:', module);
});
```

## Bước 3: Stop HTTP server và restart

```bash
# Kill the server
pkill -f "http.server 8000"

# Start again
cd /home/dai/english-learning-platform/frontend
python3 -m http.server 8000
```

Sau đó:
1. Close toàn bộ tab browser
2. Mở tab mới
3. Vào `http://localhost:8000/test-admin.html`
4. Test lại

## Bước 4: Kiểm tra file trên disk

Chạy command này để xác nhận file đã được ghi:
```bash
grep -A 5 'if (username === "admin"' /home/dai/english-learning-platform/frontend/js/mockApi.js
```

Phải thấy:
```javascript
if (username === "admin" && password === "admin123") {
  console.log('[mockApi] ✅ ADMIN LOGIN SUCCESS');
  return {
    ...
    role: "ADMIN"
```

## Bước 5: Nếu vẫn không được - workaround tạm thời

Sửa trực tiếp trong `login.html`, thêm sau `saveAuth(response);`:

```javascript
// TEMPORARY FIX: Force admin role if username is admin
if (username === 'admin' && password === 'admin123') {
  response.user.role = 'ADMIN';
  response.user.id = 999;
  response.user.email = 'admin@linguistai.com';
  saveAuth(response);
}
```
