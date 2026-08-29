# Test NavBar Component & Avatar Fix

## ✅ Đã fix:

1. **Avatar không khớp** → Login response giờ bao gồm `avatarUrl`
2. **NavBar component** → File `js/navbar.js` để tái sử dụng
3. **Admin link tự động** → NavBar tự động hiển thị link Admin cho ADMIN users

---

## 🔄 Test ngay (không cần update HTML):

### Bước 1: Clear cache và logout

```javascript
localStorage.clear();
location.reload();
```

### Bước 2: Login lại

Vào `http://localhost:8000/login.html`

**Admin account:**
- Username: `admin`
- Password: `admin123`

**User account:**
- Username: `john`
- Password: `123456`

### Bước 3: Kiểm tra localStorage

Mở Console (F12):
```javascript
const user = JSON.parse(localStorage.getItem('linguistai_user'));
console.log('User:', user);
console.log('Avatar URL:', user.avatarUrl);
```

**Admin sẽ thấy:**
```
avatarUrl: "https://picsum.photos/seed/admin/200/200"
role: "ADMIN"
```

**User sẽ thấy:**
```
avatarUrl: "https://picsum.photos/seed/johndoe/200/200"
role: "USER"
```

### Bước 4: Kiểm tra avatar trong profile

Vào `http://localhost:8000/profile.html`

Avatar hiển thị phải khớp với URL trong localStorage.

---

## 📝 Next Steps (Optional - để clean code hơn):

Nếu muốn dùng NavBar component cho tất cả trang, tôi có thể update:
- chat.html
- topics.html
- topic-detail.html
- quiz.html
- progress.html
- profile.html
- admin.html

Thay thế inline navbar HTML bằng 2 dòng:
```javascript
import { initNavBar } from './js/navbar.js';
initNavBar('chat'); // current page name
```

Điều này sẽ:
- ✅ Giảm code duplicate
- ✅ Admin link tự động hiển thị
- ✅ Avatar tự động đồng bộ
- ✅ Dễ maintain khi có thay đổi navbar

Bạn muốn tôi update toàn bộ trang ngay không?
