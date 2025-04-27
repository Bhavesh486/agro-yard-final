# Testing Instructions for User Type-based UI with Firestore Integration

## Overview
This feature shows different screens based on the user type:
- Farmer users see a UI with: Crop Info, Betting, Upload Product, and Receive Delivery
- Member users see a UI with: Products, Betting, Payment, and Delivery
- **NEW**: The bottom navigation "Payment" tab shows different content based on user type:
  - For Farmers: Shows "Payment History" with a list of received payments
  - For Members: Shows the payment form to send payments
- **NEW**: User data is now stored in Firebase Firestore for persistence across devices

## Firebase Firestore Integration

The app now stores user information in Firebase Firestore during signup:
- When a user signs up, their data is saved to Firestore in the "users" collection
- During login, the app retrieves the user's data from Firestore
- If Firestore data is unavailable, the app falls back to SharedPreferences or email-based detection
- User data stored includes: name, email, mobile, userType, state, district, and market details (for members)

## How to Test

1. **Debug Mode**:
   The app now includes debug logs to help diagnose user type issues:
   - When in Android Studio, you can view the following log tags:
     - `SignupUser` - Shows what user type was saved during signup and Firestore operations
     - `LoginUsers` - Shows stored user type and Firestore data retrieval results
     - `HomeFragment` - Shows what user type is being used for the UI
     - `BottomNavigation` - Shows what user type is being used for navigation

2. **Check Firestore Database**:
   - After signing up, you can check the Firebase Console to see the user data stored
   - Navigate to your Firebase project > Firestore Database > users collection
   - Each user document will have a UID matching their Firebase Authentication UID
   - Verify that all user data was correctly saved, especially the userType field

3. **Signup as a Member**:
   - Open the app
   - Click "Don't have an account? Sign Up"
   - Fill in the required fields
   - In the "Select User Type" dropdown, choose "Member"
   - Fill in the Market Name and Market ID fields
   - Select State and District
   - Complete the signup process
   - You should be redirected to the login screen
   - Your data will be stored in Firestore

4. **Login with Member credentials**:
   - Enter the email and password you used to sign up
   - Click Login
   - The app will fetch your user data from Firestore
   - You should see:
     - The welcome message "Welcome, [Your Name] Member"
     - Four cards: Products, Betting, Payment, Delivery
     - The bottom navigation will have "Payment" tab
     - Tapping on "Payment" will show a form to send payment to farmers

5. **Login with a Farmer account**:
   - Open the app
   - Signup with a new account, selecting "Farmer" as the user type
   - Login with those credentials
   - You should see:
     - The welcome message "Welcome, Farmer [Your Name]"
     - Four cards: Crop Info, Betting, Upload Product, Receive Delivery
     - The bottom navigation will have "Payment History" instead of "Payment"
     - Tapping on "Payment History" will show a list of payments received by the farmer

## Implementation Details

The user type detection and storage now happens in multiple layers:
1. During signup, user data is stored in Firebase Firestore and temporarily in SharedPreferences
2. During login, the app first tries to retrieve the user type from Firestore
3. If Firestore data is unavailable, it falls back to SharedPreferences
4. If neither is available, it uses email-based detection as a last resort

## Notes
- Firestore provides cloud-based data persistence, allowing users to access their profiles across devices
- The same UI adaptation logic continues to work based on the retrieved user type
- In a production app, you would add more robust error handling and loading indicators

## Notes
- This is a demo implementation. In a real-world scenario, the user type would be determined during actual authentication.
- The code intelligently adapts the UI based on the stored user type.
- The weather forecast section is identical for both user types. 