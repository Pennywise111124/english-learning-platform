## Commit Message

```bash
git add .
git commit -m "feat: add shared NavBar component with logout and admin features

- Created reusable NavBar component (js/navbar.js)
- All 7 authenticated pages now use shared navbar
- Admin link auto shows/hides based on user role
- Removed Profile link from navbar (access via avatar only)
- Added logout button with confirmation popup
- Fixed avatar sync between navbar and profile page
- Fixed admin page tabs selector
- Fixed topics loading on initial admin page load
- Added admin test account (username: admin, password: admin123)
- Cleaned up 24 test/debug files

Co-Authored-By: Claude Code <noreply@anthropic.com>"

git push origin feature/frontend
```

## Summary

✅ **NavBar Component**: Tất cả trang dùng chung 1 navbar
✅ **Admin Link**: Tự động show/hide theo role
✅ **No Profile Link**: Chỉ vào profile qua avatar
✅ **Avatar Sync**: Giống nhau trên mọi trang
✅ **Logout Confirmation**: Popup xác nhận trước khi logout
✅ **Admin Tabs Fixed**: Topics/Flashcards/Quizzes hoạt động đúng
✅ **Topics Loading Fixed**: Hiển thị ngay khi vào admin page
✅ **Project Clean**: Đã xóa tất cả test/debug files

## Production Files

**Frontend HTML** (10 pages):
- index.html, login.html, register.html
- chat.html, topics.html, topic-detail.html, quiz.html
- progress.html, admin.html, profile.html

**JavaScript** (4 modules):
- js/auth.js, js/api.js, js/mockApi.js
- js/navbar.js (NEW - shared component)

**Ready to push!** 🚀
