# PROG7314 Part 3 - UniVerse Student Survival App

# Students: ST10321423
# Module: Programming 3D (PROG7314)

## Overview
The UniVerse Task Management System is an Android app that lets students add, edit, delete, and track daily tasks and habits. It uses Firebase Authentication and Cloud Firestore to deliver secure sign-in, real-time synchronization, and resilient offline behavior.

## Solution Components
### Frontend (Android)
- Built with Android Studio (Kotlin)
- Activities/fragments with a user-friendly task and habit workflow
- Multi-language ready through Android string resources
- Accessibility-friendly UI with standard components for TalkBack/VoiceOver and sensible contrast defaults
- Real-time data display backed by Firestore listeners

### Backend (Firebase Cloud)
- Firebase Authentication for secure login (Google Sign-In and Email/Password)
- Cloud Firestore for scalable, structured task and habit data
- Real-time synchronization of user data across devices

### Pipeline
- Version control with GitHub
- Local builds and testing through Android Studio
- Ready for GitHub Actions CI/CD automation
- Branching strategy: `main`, `dev`, and `feature/*`

## Security Features
### User Authentication
- Firebase Authentication for Google Sign-In and Email/Password
- Automatic token-based session management

### Password and Account Protection
- Credentials encrypted by Firebase Auth; no plaintext storage
- OAuth2 flow for Google Sign-In

### Data Protection
- Firestore Security Rules restrict access to documents where `userId == request.auth.uid`
- All reads and writes require authenticated sessions

### Input Validation
- Task and habit fields validated before Firestore writes
- Empty or malformed inputs blocked on both frontend and backend

### Protection Against Common Attacks
- HTTPS (TLS) for encrypted transmission
- Firebase infrastructure mitigates XSS, SQL injection, and CSRF
- Firestore rules prevent privilege escalation; auth tokens reduce session hijacking risk

### DevSecOps Pipeline
- GitHub for collaboration and versioning
- Firebase hosting/monitoring for backend components
- Local unit testing before commits; CI pipeline ready for build/test/deploy automation

## Functional Portal
### Splash Screen
- Displays the UniVerse logo while checking authentication state
- Routes to Login if signed out, or Home Dashboard if already authenticated

### Login Page
- Secure sign-in with Google or Email/Password
- Successful login updates Firebase Auth and routes to the Home Dashboard

### Home / Dashboard
- Personalized welcome with metrics (tasks completed, active habits)
- Shortcuts to Tasks, Habits, and Settings

### Tasks Feature
- Add, edit, or delete tasks with title, description, and optional due date
- Tasks stored in Firestore and shown in a RecyclerView with live updates

### Habits Feature
- Add, edit, or delete daily habits (e.g., study goals, routines)
- Track completions per day with Firestore-backed analytics
- Dashboard reflects progress dynamically

### Settings Feature
- Toggle Light, Dark, or System Default theme
- Preference saved locally and applied at startup
- Logout option securely signs out of Firebase and returns to Login

### Logout / Session Handling
- Firebase clears session and cached data on logout
- App returns to the Login Page for next sign-in

## Task API Overview
Bridges the UI with Firestore to keep data secure and synchronized.

### Data Model
- **Collection:** `tasks`
- **Fields:**
  - `taskId`: Unique identifier (auto-generated)
  - `userId`: UID of the authenticated user
  - `title`: Task title
  - `description`: Task details
  - `dueDate`: Firestore Timestamp
  - `isCompleted`: Boolean task status

### Core Functions
- `addTask()`: Add a new task for the logged-in user
- `updateTask()`: Update existing task data
- `deleteTask()`: Remove a task from Firestore
- `fetchTasksByUser()`: Retrieve all tasks for a given `userId`
- `syncTasks()`: Keep tasks synchronized in real time with Firestore listeners

### Data Flow
1. Retrieve `currentUser.uid` from Firebase Auth.
2. Query Firestore tasks where `userId == currentUser.uid`.
3. Real-time listeners push add/edit/delete changes to the app instantly.

### Offline Behavior
- Firestore local persistence keeps recent task and habit data available offline and resyncs automatically on reconnect.
- Pending writes queue locally to prevent data loss during connectivity drops.
- UI surfaces cached data first, then refreshes when network connectivity returns.

### Localization and Offline Tips
- **Localization:** Add translated strings to locale-specific `res/values-<lang>/strings.xml` files; Android will auto-select the right language based on the device locale.
- **Right-to-left (RTL) readiness:** Use `Start`/`End` layout attributes and avoid hardcoded alignment to keep RTL layouts usable.
- **Offline UX:** Show cached tasks/habits instantly, then merge server updates when back online. Consider lightweight toasts/snackbars for queued writes and sync status.

## Installation and Running the Project
### Clone Repository
```bash
git clone https://github.com/ST10321423/PROG7314_Team_Repo.git

```

### Setup in Android Studio
1. Open the project in Android Studio.
2. Connect Firebase: **Tools → Firebase → Authentication → Connect to Firebase**.
3. Enable Email/Password and Google Sign-In.
4. Enable Cloud Firestore and sync Gradle.
5. Run the app on an emulator or connected device.

**App:** Android Emulator / Device  
**Backend:** Firebase (Firestore + Auth)

## References
- Firebase (2025). Authentication and Firestore Documentation. https://firebase.google.com/docs
- Google Developers (2025). Best Practices for Secure Android Development. https://developer.android.com/topic/security/best-practices
- Firebase (2025). Cloud Firestore Security Rules. https://firebase.google.com/docs/firestore/security/rules-conditions

<img width="844" height="564" alt="image" src="https://github.com/user-attachments/assets/ed720869-38b2-4a43-9931-17d342ac318e" />

<img width="1600" height="520" alt="image" src="https://github.com/user-attachments/assets/2402721a-0526-41b8-9208-ebf715ea7c38" />

<img width="1224" height="661" alt="image" src="https://github.com/user-attachments/assets/c0e632ff-dcf7-4712-9c32-32ffb1dc185a" />


