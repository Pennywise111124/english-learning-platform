# Hướng dẫn Test Tài khoản Admin

## Bước 1: Xóa localStorage cũ

Mở Console trình duyệt (F12) và chạy:

```javascript
localStorage.clear();
location.reload();
```

## Bước 2: Login lại

Truy cập: `http://localhost:8000/login.html`

**Tài khoản Admin:**
- Username: `admin`
- Password: `admin123`

## Bước 3: Kiểm tra role trong Console

Sau khi login thành công, mở Console (F12) và chạy:

```javascript
const user = JSON.parse(localStorage.getItem('linguistai_user'));
console.log('User:', user);
console.log('Role:', user.role);
```

Kết quả phải là:
```javascript
User: {id: 999, username: "admin", email: "admin@linguistai.com", role: "ADMIN"}
Role: "ADMIN"
```

## Bước 4: Kiểm tra link Admin

Nếu role đúng là `ADMIN`, link "Admin" sẽ tự động xuất hiện trong navbar.

Nếu không thấy link "Admin", mở Console và chạy:

```javascript
document.getElementById('admin-nav-link')?.classList.remove('hidden');
```

## Debug Steps

Nếu vẫn không hoạt động:

1. **Kiểm tra mockApi.js đang load đúng:**
```javascript
import { login } from './js/api.js';
const result = await login('admin', 'admin123');
console.log('Login result:', result);
```

2. **Kiểm tra admin-nav-link có tồn tại:**
```javascript
console.log('Admin link:', document.getElementById('admin-nav-link'));
```

3. **Force show admin link:**
```javascript
const user = JSON.parse(localStorage.getItem('linguistai_user'));
user.role = 'ADMIN';
localStorage.setItem('linguistai_user', JSON.stringify(user));
document.getElementById('admin-nav-link')?.classList.remove('hidden');
```

## Tài khoản User thường (để so sánh)

- Username: `john`
- Password: `123456`
- Role sẽ là: `USER`
- Link "Admin" sẽ KHÔNG hiển thị

---

## Note

Nếu bạn đã login trước đó với tài khoản khác, localStorage vẫn giữ thông tin cũ. **PHẢI xóa localStorage trước khi test lại.**
