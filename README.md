# HallSync

A beginner-friendly JavaFX + SQLite desktop application for university residential hall management.

## Included
- Unified login for Provost, Student, Staff and Admin
- Role-based dashboards
- Notice board first on every dashboard
- Itemized rent, meals and electricity bills
- PDF bill export using a tiny built-in PDF writer (no PDF framework)
- Student meal calendar with 24-hour rule
- Green / grey / red meal states
- Admin meal shutdown (Hall OFF)
- Complaints and admin resolution
- Searchable read-only student directory
- Hall achievements, active Provost, former Provost archive
- Dining manager information
- Student committee with public-profile popup
- MVC-style package structure

## Demo accounts
- admin / admin
- provost / provost
- student / student
- staff / staff

## Requirements
- JDK 17 or newer
- IntelliJ IDEA
- Internet once, so IntelliJ/Maven can download JavaFX and SQLite JDBC

## Important
The SQLite database file `hallsync.db` is created automatically beside the project when the app first starts.
