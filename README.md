PROG7314 Part 2 - UniVerse Student Survival App

Students: ST10321423
Module: Programming 3D (PROG7314)

Overview

This project implements a secure and efficient Task Management System for the UniVerse Android application. The system allows users to manage their daily tasks — add, edit, delete, and track progress — all integrated with Firebase for real-time updates and secure authentication.

The solution consists of two main components:

Frontend (Mobile App)
Built using Android Studio (Kotlin)
User-friendly interface for task management
Seamless navigation through activities and fragments
Real-time updates with Firebase Firestore

Backend (Firebase Cloud)
Firebase Authentication for secure login
Cloud Firestore for structured and scalable data storage
Real-time synchronization of user tasks across devices

Pipeline:
Version control managed via GitHub
Local builds and testing performed through Android Studio
Ready for integration with GitHub Actions for CI/CD automation
Structured branching: main, dev, and feature/* for team collaboration

Security Features:

User Authentication
Secure login using Firebase Authentication (Google Sign-In / Email & Password)
Firebase handles token-based session management automatically

Password and Account Protection
Firebase Auth ensures all credentials are encrypted
No plaintext password storage
Google Sign-In uses OAuth2 for secure third-party authentication

Data Protection
Firestore data secured with Firebase Security Rules
Users can only access documents where userId == request.auth.uid
All reads and writes go through authenticated sessions

Input Validation
Task names and descriptions validated before Firestore write
Empty or malformed inputs blocked in both frontend and backend

Protection Against Common Attacks
HTTPS (TLS) ensures encrypted data transmission
Firebase’s backend infrastructure mitigates XSS, SQLi, and CSRF
Firestore Security Rules prevent privilege escalation
Authentication tokens protect against session hijacking

DevSecOps Pipeline
Versioning and collaboration through GitHub
Firebase hosting and monitoring for backend components
Code undergoes local unit testing before commits
CI pipeline ready for future automation (build/test/deploy)

Functional Portal
User Flow

Splash Screen:
Displays the UniVerse logo briefly while checking the user’s authentication state.
Automatically navigates to the Login Screen if not signed in, or directly to the Home Dashboard if the user is already authenticated.

Login Page:
Users can log in securely using Google Sign-In or Email/Password authentication.
Successful sign-in creates or updates the user record in Firebase Authentication and redirects to the Home Dashboard.

Home / Dashboard:
Shows a personalized welcome message and key metrics like tasks completed and active habits.
Provides navigation shortcuts to Tasks, Habits, and Settings screens.

Tasks Feature:
Users can add, edit, or delete tasks.
Each task includes a title, description, and optional due date.
Tasks are stored in Firestore and displayed in a RecyclerView, updating in real time.

Habits Feature:
Users can add, edit, or delete daily habits such as study goals or routines.
Completed habits are tracked per day and synced with Firestore for progress analytics.
Data updates dynamically on the dashboard.

Settings Feature:
Allows users to switch between Light Mode, Dark Mode, or System Default Theme.
The chosen theme preference is saved locally and automatically applied at app startup.
Also includes a Logout option that securely signs the user out of Firebase and returns them to the Login Screen.

Logout / Session Handling
On logout, Firebase clears the user’s session and cached data.
The app returns to the Login Page for the next sign-in.

Task API Overview:
The Task API acts as a bridge between the app’s frontend (UI) and Firestore (backend).
It ensures smooth and secure data flow using Firebase services.

Structure:
Collection: tasks
Document Fields:
taskId: Unique identifier (auto-generated)
userId: UID of the authenticated user
title: Task title
description: Task details
dueDate: Firestore Timestamp
isCompleted: Boolean (task status)

Core Functions
	
addTask():	Adds a new task for the logged-in user
updateTask():	Updates existing task data
deleteTask():	Removes a task from Firestore
fetchTasksByUser():	Retrieves all tasks for a specific userId
syncTasks():	Keeps tasks synced in real time using Firestore listeners

Data Flow
The app retrieves the currentUser.uid from Firebase Auth.
Firestore queries tasks where userId == currentUser.uid.
Any change in Firestore (add/edit/delete) updates the app instantly via real-time listeners.

Installation & Running the Project
Clone Repository

git clone https://github.com/ST10321423/PROG7314_Team_Repo.git

Setup and Run in Android Studio:

-Open project in Android Studio
-Connect Firebase:
-Tools → Firebase → Authentication → Connect to Firebase
-Enable Email/Password & Google Sign-In
-Enable Cloud Firestore
-Sync Gradle
-Run app on emulator or connected device

App → Android Emulator / Device
Backend → Firebase (Firestore + Auth)

References
Firebase (2025). Authentication and Firestore Documentation.
https://firebase.google.com/docs
Google Developers (2025). Best Practices for Secure Android Development.
https://developer.android.com/topic/security/best-practices
Firebase (2025). Cloud Firestore Security Rules.
https://firebase.google.com/docs/firestore/security/rules-conditions
