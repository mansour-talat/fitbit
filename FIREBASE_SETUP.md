# Firebase Setup Instructions

## Security Rules Deployment

The app requires proper Firebase security rules to function correctly. Follow these steps to deploy the rules:

### Option 1: Using Firebase Console (Recommended for beginners)

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Firestore Database > Rules
4. Copy the contents of `firestore.rules` file
5. Paste the rules into the editor
6. Click "Publish"

### Option 2: Using Firebase CLI

1. Install Firebase CLI if you haven't already:
   ```
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```
   firebase login
   ```

3. Initialize Firebase in your project (if not already done):
   ```
   firebase init
   ```
   - Select Firestore
   - Choose your project

4. Run the deployment script:
   ```
   .\deploy_rules.ps1
   ```

## Security Rules Explanation

The security rules implement the following permissions:

1. **Users Collection**: Users can read and write their own documents
2. **Trainer Collection**: Trainers can read and write their own documents
3. **Trainer Clients Collection**: 
   - Trainers can read and write documents where they are the trainer
   - Clients can read documents where they are the client
4. **Default**: All other access is denied by default

## Troubleshooting

If you're still experiencing permission issues after deploying the rules:

1. Verify that the user is properly authenticated
2. Check that the user document exists in the "trainer" collection
3. Ensure the "trainerId" field in client documents matches the authenticated user's ID
4. Check Firebase Console logs for specific permission errors 