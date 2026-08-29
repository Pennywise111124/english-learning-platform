# Quick Test - NavBar Component

Đã update `topics.html` để dùng NavBar component.

## Test ngay:

1. **Logout và login lại:**
```javascript
localStorage.clear();
location.reload();
```

2. **Login:**
- Admin: `admin` / `admin123`
- User: `john` / `123456`

3. **Vào topics:** `http://localhost:8000/topics.html`

4. **Mở Console (F12)** - sẽ thấy logs:
```
[NavBar] Initializing with user: {id: 999, username: "admin", role: "ADMIN", avatarUrl: "..."}
[NavBar] ✅ Admin link added
[NavBar] Avatar URL: https://picsum.photos/seed/admin/200/200
[NavBar] Rendered into #navbar-container
```

5. **Kiểm tra:**
- ✅ Link "Admin" hiển thị (nếu login bằng admin)
- ✅ Avatar hiển thị đúng ảnh
- ✅ Không có link "Profile" trong navbar
- ✅ Click avatar → vào profile page

## Nếu OK, tôi sẽ update 6 trang còn lại:
- chat.html
- quiz.html
- topic-detail.html
- progress.html
- profile.html
- admin.html

Báo tôi kết quả test topics.html trước nhé!
