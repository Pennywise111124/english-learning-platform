# English Learning Platform - Frontend

This directory contains the frontend application for the LinguistAI platform.

## Structure

```
frontend/
├── index.html          # Landing page
├── login.html          # Login page
├── register.html       # Registration page
├── chat.html           # Chat interface
├── topics.html         # Topics browser
├── topic-detail.html   # Topic detail with flashcards
├── quiz.html           # Quiz page
├── progress.html       # User progress tracking
├── admin.html          # Admin panel (requires ADMIN role)
├── profile.html        # User profile settings
├── css/
│   └── design-tokens.css  # Material Design 3 tokens
└── js/
    ├── auth.js         # Authentication utilities
    ├── api.js          # API wrapper (uses mockApi)
    ├── mockApi.js      # Mock backend API
    └── navbar.js       # Shared NavBar component

## Features

- **Authentication**: JWT-based with localStorage
- **Role-based Access**: USER and ADMIN roles
- **Mock API**: Frontend-first development without backend
- **Responsive Design**: Mobile-first with Tailwind CSS
- **Component Architecture**: Reusable NavBar component

## Admin Account

For testing admin features:
- Username: `admin`
- Password: `admin123`

## NavBar Component

All authenticated pages use the shared NavBar component (`js/navbar.js`):
- Automatically shows/hides Admin link based on role
- Avatar click → Profile page
- Logout button with confirmation
- No "Profile" link in navbar

## Development

1. Serve with any HTTP server:
   ```bash
   python3 -m http.server 8080
   ```

2. Open `http://localhost:8080/login.html`

3. Login with admin account or any credentials (mock API accepts all)

## Notes

- Uses CDN for Tailwind CSS and Material Icons
- All pages require authentication (except login/register)
- Mock API returns realistic data for testing
