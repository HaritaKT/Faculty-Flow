# FacultyFlow - Android Application

FacultyFlow is a modern Android application for managing student-faculty interactions on campus. It helps students discover faculty members, view availability, request appointments, and track responses, while faculty members can upload timetables, manage booking requests, update profiles, and communicate replies.

The app is built with Kotlin, XML layouts, Firebase, Firestore, and a lightweight AI timetable-scanning workflow. It is designed around a dark, vibrant mobile interface with clear visual hierarchy, role-based flows, and presentation-ready screens for both student and faculty users.

## Overview

FacultyFlow solves a common campus coordination problem: students often do not know when faculty members are available, and faculty members need a simple way to manage meeting requests around their teaching schedule.

The app provides:

- Role-based signup and login for students and faculty.
- Faculty discovery with search, filters, and availability indicators.
- Student appointment requests with date, time, and meeting note.
- Faculty request inbox with accept, decline, and reply support.
- AI-assisted timetable scanning from image/PDF uploads.
- Timetable-aware blocking of busy slots on the student booking screen.
- Student and faculty profile pages with logout support.
- A dark, vibrant design system optimized for mobile screens.

## User Roles

### Student

Students can:

- Sign up using an `@rvu.edu.in` email address.
- Select degree and semester during signup.
- Browse faculty members from the faculty directory.
- Search faculty by name or department.
- Filter faculty by department.
- View faculty profile details such as designation, room/block, office hours, and availability.
- Select a date and available time slot.
- Add a purpose/note for the meeting.
- Submit booking requests to faculty.
- Track booking status from My Bookings.
- View faculty replies.
- Open a dedicated student profile page.
- Sign out from the profile page.

### Faculty

Faculty members can:

- Sign up and log in using an `@rvu.edu.in` email address.
- View a home dashboard with greeting, availability status, pending requests, and schedule progress.
- Toggle manual busy/available status.
- Upload weekly timetable images or PDFs.
- Review AI-detected class/busy slots before syncing.
- Save timetable slots to Firestore.
- View daily schedule timelines from Monday to Friday.
- Receive student booking requests.
- Accept or decline requests.
- Add an optional reply for students.
- Edit profile information such as name, designation, department, room/block, office hours, and profile photo.
- Sign out from the profile editor.

## Key Features

### Authentication

- Firebase Authentication is used for signup and login.
- Email validation requires the RVU domain: `@rvu.edu.in`.
- User session data is cached locally using `SharedPreferences`.
- App launch routing sends logged-in users directly to the correct dashboard.

### Student Faculty Directory

- Displays faculty cards from Firestore.
- Supports real-time updates through Firestore snapshot listeners.
- Shows faculty name, designation, department, and live availability state.
- Includes search and department filters.
- Uses bottom navigation for directory, bookings, and profile.

### Faculty Profile View

- Shows faculty information to students.
- Displays current availability using color-coded indicators.
- Shows location and office hours.
- Provides quick access to appointment booking.

### Booking Flow

- Students choose from the next 7 days.
- Time slots are generated as a visual 30-minute grid from 9 AM to 5 PM.
- Busy faculty slots are blocked using timetable overlap detection.
- Students can add a meeting note.
- Booking requests are saved to Firestore with `pending` status.

### My Bookings

- Students can view all submitted bookings.
- Booking cards expand to show notes and faculty replies.
- Tabs filter bookings by:
  - All
  - Pending
  - Upcoming
  - Done
- Status colors distinguish pending, confirmed, declined, and completed bookings.

### Faculty Home Dashboard

- Shows faculty greeting and profile image.
- Displays current manual availability status.
- Shows pending booking count.
- Shows schedule/day progress.
- Displays timetable entries for selected weekday.
- Uses day chips for Monday to Friday timelines.

### Timetable Upload and AI Scanning

Faculty can upload a timetable as an image or PDF. The app converts the uploaded file into bitmap data and sends it to Gemini Vision through the app's timetable scanner.

The scanner extracts:

- Day
- Time range
- Subject/class name
- Room, if visible

The extracted slots are saved per weekday in Firestore:

```text
timetables/{facultyUid}
|-- monday: ["09:10 - 10:10 | MADE | N/A", ...]
|-- tuesday: [...]
|-- wednesday: [...]
|-- thursday: [...]
`-- friday: [...]
```

The student booking screen reads these timetable entries and blocks overlapping booking slots.

### Faculty Booking Inbox

- Faculty members see pending requests only.
- Requests are sorted by date/time.
- Each request shows student name, requested time, and note preview.
- Faculty can expand cards to read full notes.
- Accept and decline actions update Firestore.
- Optional replies are saved as `facultyReply` and shown to students.

### Profiles and Logout

Student profile:

- Name
- Email
- Degree
- Semester
- My Bookings shortcut
- Sign Out button

Faculty profile editor:

- Name
- Designation
- Department
- Office room/block
- Office hours
- Profile photo
- Sign Out button

## Design System

FacultyFlow uses a dark, vibrant mobile-first design inspired by premium event and productivity apps.

### Visual Direction

- Deep dark gradient backgrounds.
- Elevated dark cards with subtle borders.
- Blue, purple, teal, green, amber, and red accents.
- Condensed bold typography for important headings and high-emphasis labels.
- Clean sans-serif body text for readability.
- Rounded controls and card-based layouts.
- Clear color-coded statuses for availability and booking state.

### Color Roles

- Green: available/confirmed states.
- Amber/orange: busy/pending states.
- Red/pink: declined/error/sign-out states.
- Blue/purple/teal: primary actions, selected controls, profile accents.
- Dark surfaces: app background, cards, input areas, and navigation surfaces.

### Typography

- Headings use bold condensed styling for a strong campus-poster feel.
- Buttons, tabs, chips, and badges use consistent condensed bold styling.
- Body text remains standard sans-serif for readability on mobile screens.
- Text sizes are kept mobile-safe to avoid overflow or misalignment.

## Technical Stack

- Language: Kotlin
- UI: XML layouts
- View system: ViewBinding
- Backend: Firebase Authentication and Cloud Firestore
- Storage/Image handling: Firebase Storage dependency, ImgBB upload flow, Glide image loading
- AI: Gemini Vision API for timetable parsing
- Async: Kotlin Coroutines
- Networking: OkHttp
- OCR/vision support: ML Kit Text Recognition dependency
- UI components: Material Design Components, AndroidX, RecyclerView, CardView
- Circular images: CircleImageView

## Architecture

The app uses a simple Activity-based architecture with separated packages for faculty, student, AI helpers, models, adapters, and utilities.

```text
app/src/main/java/com/example/madecie3/
|-- MainActivity.kt
|-- LoginActivity.kt
|-- SignupActivity.kt
|-- ai/
|   |-- SmartEngine.kt
|   |-- TimetableScanner.kt
|   |-- TimetableParser.kt
|   `-- AIRequestSorter.kt
|-- faculty/
|   |-- FacultyHomeActivity.kt
|   |-- TimetableUploadActivity.kt
|   |-- BookingInboxActivity.kt
|   |-- ProfileEditorActivity.kt
|   |-- adapters/
|   `-- models/
|-- student/
|   |-- FacultyDirectoryActivity.kt
|   |-- FacultyProfileActivity.kt
|   |-- BookSlotActivity.kt
|   |-- MyBookingsActivity.kt
|   |-- StudentProfileActivity.kt
|   |-- adapters/
|   `-- models/
|-- utils/
|   |-- Constants.kt
|   |-- Extensions.kt
|   `-- PreferencesManager.kt
`-- ui/theme/
```

## Firestore Data Model

### users

Stores both student and faculty profiles.

Student example:

```text
users/{studentUid}
|-- uid
|-- name
|-- email
|-- userType: "student"
|-- degree
`-- semester
```

Faculty example:

```text
users/{facultyUid}
|-- uid
|-- name
|-- email
|-- userType: "faculty"
|-- designation
|-- department
|-- roomBlock
|-- officeHours
|-- availability
`-- profileImageUrl
```

### bookings

Stores student appointment requests.

```text
bookings/{bookingId}
|-- studentId
|-- studentName
|-- facultyId
|-- facultyName
|-- facultyDesignation
|-- date
|-- timeSlot
|-- status
|-- studentNote
|-- facultyReply
`-- timestamp
```

Supported booking statuses:

- `pending`
- `confirmed`
- `declined`
- `done`

### timetables

Stores faculty busy/class slots.

```text
timetables/{facultyUid}
|-- monday
|-- tuesday
|-- wednesday
|-- thursday
`-- friday
```

Each day contains a list of formatted slot strings:

```text
"09:10 - 10:10 | Subject Name | Room"
```

## Important Screens

### Splash and Routing

`MainActivity` displays the splash screen, checks login state, and routes users to either:

- Student faculty directory
- Faculty home dashboard
- Login screen

### Login

`LoginActivity` authenticates users through Firebase Auth and loads profile data from Firestore.

### Signup

`SignupActivity` supports student/faculty selection and dynamic student fields.

### Faculty Directory

`FacultyDirectoryActivity` is the student landing screen after login.

### Book Slot

`BookSlotActivity` handles date selection, slot display, busy-slot blocking, note entry, and Firestore booking creation.

### Faculty Home

`FacultyHomeActivity` displays the faculty schedule dashboard and pending request summary.

### Timetable Upload

`TimetableUploadActivity` handles file picking, AI scanning, slot review, and timetable sync.

### Booking Inbox

`BookingInboxActivity` lets faculty accept or decline pending student requests.

## Setup

### Requirements

- Android Studio
- Android SDK 24 or higher
- JDK 17
- Firebase project
- `google-services.json` configured for package `com.example.madecie3`
- Gemini API key for timetable scanning
- ImgBB API key for profile image uploads

### Local Configuration

Create or update `local.properties` at the project root:

```properties
sdk.dir=/path/to/Android/Sdk
GEMINI_API_KEY=your_gemini_api_key
IMGBB_API_KEY=your_imgbb_api_key
```

`local.properties` is intentionally ignored by Git so API keys are not committed.

### Firebase Configuration

1. Create a Firebase project.
2. Add an Android app with package name:

```text
com.example.madecie3
```

3. Download `google-services.json`.
4. Place it in:

```text
app/google-services.json
```

5. Enable Firebase Authentication with email/password.
6. Enable Cloud Firestore.

## Running the App

1. Open the project in Android Studio.
2. Sync Gradle.
3. Add required local properties and Firebase config.
4. Run the app on an emulator or physical Android device.
5. Start from signup or login.

Command-line build:

```bash
./gradlew assembleDebug
```

If `gradlew` does not have execute permission on macOS/Linux:

```bash
sh gradlew assembleDebug
```

## Dependencies

Main dependency groups:

- AndroidX Core, AppCompat, ConstraintLayout
- Material Design Components
- Firebase Auth
- Firebase Firestore
- Firebase Storage
- Firebase Analytics
- Glide
- CircleImageView
- ML Kit Text Recognition
- Kotlin Coroutines
- OkHttp
- Jetpack Compose dependencies are present, although the current UI is XML-based

## Current Notes

- The app currently targets RVU email validation using `@rvu.edu.in`.
- Timetable scanning depends on the Gemini API key configured locally.
- Faculty image upload depends on the ImgBB API key configured locally.
- The Firebase Android API key inside `google-services.json` is expected for Firebase Android apps.
- Build output, local SDK paths, IDE cache, and local API keys should not be committed.

## Future Improvements

Potential next steps:

- Add push notifications for booking updates.
- Add faculty-side calendar export.
- Add student-side booking cancellation.
- Add admin moderation for departments and faculty records.
- Improve timetable parsing fallback for unusual timetable formats.
- Add Firestore security rules tailored to student/faculty roles.
- Add unit tests for time-slot overlap detection.
- Add UI screenshots to this README.

## Project Status

FacultyFlow is presentation-ready as a functional Android prototype with:

- Real Firebase authentication.
- Firestore-backed users, bookings, and timetables.
- AI-assisted timetable extraction.
- Student and faculty role flows.
- Timetable-aware slot blocking.
- Dark vibrant UI styling.
- Dedicated profile and logout experiences for both roles.
