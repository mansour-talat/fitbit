# PowerShell script to deploy Firebase security rules
# Make sure you have Firebase CLI installed and are logged in

Write-Host "Deploying Firebase security rules..." -ForegroundColor Green

# Deploy Firestore rules
Write-Host "Deploying Firestore rules..." -ForegroundColor Yellow
firebase deploy --only firestore:rules

# Deploy Storage rules
Write-Host "Deploying Storage rules..." -ForegroundColor Yellow
firebase deploy --only storage:rules

Write-Host "Deployment complete!" -ForegroundColor Green 