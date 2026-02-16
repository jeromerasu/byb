# BYB Mobile (Expo + React Native)

iOS-first mobile frontend MVP for the BYB backend.

## What this includes
- Login screen (JWT auth against `/api/v1/auth/login`)
- User profile intake: gender, weight, age
- Workout profile questions: equipment + minutes/day
- Calls workout generation endpoint and shows result
- Diet intake questions: physique goals, dietary limitations, preferred proteins
- Calls diet generation endpoint and shows result
- Saves generated workout/diet responses into an on-device `object-storage` folder (JSON files)

## Run locally
```bash
cd mobile
npm install
npm run start
```

Then scan the QR with Expo Go on your iPhone.

## Backend URL
In `App.tsx`, update:
```ts
const API_BASE_URL = 'http://localhost:8080';
```
Use your machine LAN IP for phone testing, e.g.:
`http://192.168.1.50:8080`

## Next recommended improvements
1. Replace single-file flow with navigation stack + typed screens
2. Add registration screen (`/api/v1/auth/register`)
3. Persist JWT securely (SecureStore)
4. Add validations and better error states
5. Add cloud object storage upload endpoint in backend (S3/GCS/Supabase Storage)
