# Firebase Setup Instructions

Follow these steps to set up Firebase Authentication for your Gym app:

## 1. Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click on "Add project" or select an existing project
3. Follow the setup wizard to create your project
4. Accept the Firebase terms if prompted

## 2. Register your Android App

1. In the Firebase Console, select your project
2. Click on the Android icon (</>) to add an Android app
3. Enter your package name: `com.example.gym`
4. Enter a nickname (optional)
5. Enter your SHA-1 signing certificate (for Google Sign-In if you want to use it later)
6. Click "Register app"

## 3. Download and Add the Configuration File

1. Download the `google-services.json` file
2. Place this file in the `app/` directory of your project (replace the placeholder one we created)

## 4. Enable Authentication Methods

1. In the Firebase Console, select your project
2. Go to "Authentication" from the left menu
3. Click on "Get started" or "Sign-in method" tab
4. Enable "Email/Password" authentication method
5. Save the changes

## 5. Testing the App

1. Run your app
2. Create a new account using the Registration screen
3. Log in with the created account
4. You should see your registered users in the Firebase Console under Authentication > Users

## Additional Steps (Optional)

### Enable Password Reset

The app already includes Firebase Authentication, but to set up password reset:

1. Enable the email template for password reset in Firebase Console
2. Add password reset functionality to the login screen

### Add Social Authentication

To add Google, Facebook, or other authentication methods:
1. Enable them in the Firebase Console
2. Add the necessary SDKs to your project
3. Implement the authentication flows

## Troubleshooting

- If you encounter build errors, make sure you've added the Firebase SDK correctly
- Check that your `google-services.json` file is in the correct location
- Verify your app's package name matches the one registered in Firebase
- Ensure your device or emulator has internet access 