# Admin Login Credentials

## Tài khoản Admin

Để truy cập trang Admin Panel (`/admin.html`), sử dụng tài khoản sau:

**Username:** `admin`  
**Password:** `admin123`

## Tài khoản User thường

Đăng nhập với bất kỳ username/password nào khác sẽ tạo tài khoản USER thường.

Ví dụ:
- Username: `john`
- Password: `123456`

---

## Cách truy cập Admin Panel

1. Mở trình duyệt và truy cập: `http://localhost:8000/login.html`
2. Nhập:
   - Username: `admin`
   - Password: `admin123`
3. Sau khi login thành công, link "Admin" sẽ tự động xuất hiện trong navbar
4. Click vào "Admin" để vào Admin Panel

---

## Tính năng Admin Panel

- **Topics Management**: Create, Update, Delete topics
- **Flashcards Management**: CRUD operations (planned)
- **Quizzes Management**: CRUD operations (planned)
- **Tab Interface**: Switch giữa Topics/Flashcards/Quizzes
- **Modal Forms**: Create/Edit topics với form validation
- **Table View**: Danh sách topics với action buttons

---

## Lưu ý

- Link "Admin" trong navbar chỉ hiển thị khi user có role = `ADMIN`
- Nếu login bằng tài khoản USER thường, link "Admin" sẽ bị ẩn
- Admin panel được bảo vệ bởi `checkAdmin()` guard - redirect về index.html nếu không phải ADMIN
