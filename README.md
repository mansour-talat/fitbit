# Gym App

A comprehensive fitness and workout tracking Android application that helps users manage their workouts, track nutrition, and monitor activity levels.

## Features

- 🏋️‍♂️ Workout tracking and planning
- 📊 Nutrition monitoring and meal planning
- 👣 Activity tracking (steps, distance, calories)
- 📈 Progress tracking and statistics
- 👥 User profiles and authentication
- 🔔 Custom exercise creation
- 📱 Modern Material Design UI

## Prerequisites

Before running the app, make sure you have the following installed:

- Android Studio (Latest version recommended)
- JDK 11 or higher
- Android SDK with minimum API level 24 (Android 7.0)
- Google Play Services
- Firebase account and project setup

## Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/gym.git
   cd gym
   ```

2. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Add an Android app to your Firebase project:
     - Package name: `com.su.gym`
     - Download the `google-services.json` file
     - Place `google-services.json` in the `app/` directory

3. **Firebase Services Setup**
   - Enable Authentication with Email/Password sign-in method
   - Create a Cloud Firestore database
   - Set up Firebase Storage
   - Configure Firebase Security Rules (available in the repository)

4. **Android Studio Setup**
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository and select it
   - Wait for the Gradle sync to complete

5. **Dependencies**
   All required dependencies are included in the `build.gradle` files. The app uses:
   - Firebase Authentication
   - Cloud Firestore
   - Firebase Storage
   - Android Architecture Components
   - Material Design Components
   - Other supporting libraries

## Running the App

1. **Connect a Device**
   - Connect an Android device via USB with USB debugging enabled
   - OR use an Android Emulator (API level 24 or higher)

2. **Build and Run**
   - Click the "Run" button in Android Studio (green play button)
   - Select your device/emulator
   - Wait for the app to build and install

3. **First Launch**
   - Create a new account or use test credentials:
     - Email: test@example.com
     - Password: test123

## Permissions Required

The app requires the following permissions:
- Internet access
- Activity recognition (for step counting)
- Body sensors
- Storage (for profile pictures)

## Troubleshooting

1. **Build Issues**
   - Clean and rebuild the project
   - File > Invalidate Caches / Restart
   - Sync project with Gradle files

2. **Firebase Connection Issues**
   - Verify `google-services.json` is in the correct location
   - Check Firebase console for correct package name
   - Ensure all Firebase services are enabled

3. **Step Counter Not Working**
   - Grant activity recognition permission
   - Verify device has the required sensors
   - Check if Google Play Services is up to date

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For any queries or support, please contact:
- Email: your.email@example.com
- Issue Tracker: GitHub Issues 