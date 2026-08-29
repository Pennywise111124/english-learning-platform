# Cách Test & Fix Admin Login

## Vấn đề: Browser đang cache file JS cũ

Vì bạn đã sửa `mockApi.js` nhưng browser vẫn dùng version cũ.

## Giải pháp 1: Hard Reload (Nhanh nhất)

1. Mở trang login: `http://localhost:8000/login.html`
2. **Hard reload** để xóa cache:
   - **Windows/Linux**: `Ctrl + Shift + R` hoặc `Ctrl + F5`
   - **Mac**: `Cmd + Shift + R`
3. Mở Console (F12) và chạy:
   ```javascript
   localStorage.clear();
   ```
4. Reload lại: `F5`
5. Login với:
   - Username: `admin`
   - Password: `admin123`

## Giải pháp 2: Disable Cache trong DevTools

1. Mở DevTools (F12)
2. Vào tab **Network**
3. Tick vào **"Disable cache"**
4. Giữ DevTools mở
5. Reload trang và login lại

## Giải pháp 3: Test trực tiếp API

Tôi đã tạo file test: `frontend/test-admin.html`

Truy cập: `http://localhost:8000/test-admin.html`

Click button "Test Login" để xem API có trả về đúng role ADMIN không.

## Giải pháp 4: Xóa toàn bộ cache browser

1. Mở DevTools (F12)
2. Click chuột phải vào nút Reload
3. Chọn "Empty Cache and Hard Reload"

## Sau khi login thành công với admin/admin123

Kiểm tra trong Console:

```javascript
const user = JSON.parse(localStorage.getItem('linguistai_user'));
console.log('Username:', user.username);
console.log('Role:', user.role);
```

Phải thấy:
```
Username: admin
Role: ADMIN
```

Nếu đúng, link "Admin" sẽ tự động xuất hiện trong navbar.

## Nếu vẫn không được

Chạy trực tiếp trong Console:

```javascript
// Force set admin role
const user = {
  id: 999,
  username: "admin",
  email: "admin@linguistai.com",
  role: "ADMIN"
};
localStorage.setItem('linguistai_user', JSON.stringify(user));
localStorage.setItem('linguistai_token', 'mock.jwt.admin.token');
location.reload();
```

Sau đó link "Admin" sẽ hiển thị ngay.
