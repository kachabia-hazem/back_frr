# WorkLink API Testing Guide

## ✅ Testing Results

All API endpoints have been tested successfully! Here's what was verified:

### Registered Users in MongoDB:
1. **Freelancer**: ahmed.benali@example.com (Password: Password123!)
2. **Company**: contact@techsolutions.tn (Password: CompanyPass123!)
3. **Admin**: mohamed.trabelsi@worklink.com (Password: AdminPass123!)

## 📋 How to View Data in MongoDB Compass

1. **Open MongoDB Compass**
2. **Connect to**: `mongodb://localhost:27017`
3. **Select Database**: Look for your database (probably named `worklink` or similar)
4. **View Collections**:
   - `freelancers` - Contains freelancer registrations
   - `companies` - Contains company registrations
   - `admins` - Contains admin registrations

## 🚀 How to Use Postman Collection

### Step 1: Import Collection
1. Open Postman
2. Click **Import** button (top left)
3. Select file: `WorkLink-Postman-Collection.json`
4. Click **Import**

### Step 2: Test Endpoints

The collection is organized into folders:

#### 📁 Authentication Folder
Contains all registration and login endpoints:

1. **Register Freelancer** - Creates a new freelancer account
2. **Register Company** - Creates a new company account
3. **Register Admin** - Creates a new admin account
4. **Login Freelancer** - Login with freelancer credentials
5. **Login Company** - Login with company credentials
6. **Login Admin** - Login with admin credentials

#### 📁 Protected Routes (Test) Folder
Test endpoints that require JWT authentication:

1. **Test Freelancer Access** - Requires freelancer token
2. **Test Company Access** - Requires company token
3. **Test Admin Access** - Requires admin token

#### 📁 Additional Test Users Folder
More sample users for testing:
- Frontend Developer (Sarah)
- Startup Company (InnovateTech)

### Step 3: Run Requests

**Automatic Token Management**: The collection automatically saves JWT tokens when you register or login!

1. **First**, run any registration request (e.g., "Register Freelancer")
2. **Token is saved automatically** to collection variables
3. **Then**, test protected routes - they will use the saved token

### Manual Token Usage:
If you want to manually use a token:
1. Copy the `token` from any registration/login response
2. Go to protected endpoint
3. Add header: `Authorization: Bearer YOUR_TOKEN_HERE`

## 📊 Expected Responses

### Successful Registration (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "ahmed.benali@example.com",
  "role": "FREELANCER",
  "id": "697b85f877a8b173eb4c185e",
  "message": "Freelancer enregistré avec succès"
}
```

### Successful Login (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "ahmed.benali@example.com",
  "role": "FREELANCER",
  "id": "697b85f877a8b173eb4c185e",
  "message": "Connexion réussie"
}
```

### Error Response (400/401):
```json
{
  "token": null,
  "email": null,
  "role": null,
  "id": null,
  "message": "Error description here"
}
```

## 🔑 Test Credentials

### Already Created Users:
| Type | Email | Password |
|------|-------|----------|
| Freelancer | ahmed.benali@example.com | Password123! |
| Company | contact@techsolutions.tn | CompanyPass123! |
| Admin | mohamed.trabelsi@worklink.com | AdminPass123! |

## 📝 Available Enums

### Gender:
- MALE
- FEMALE
- OTHER

### Profile Types (for Freelancers):
- BI_DATA
- BUSINESS_CONSULTING
- ERP_CRM
- INDUSTRIAL_IT_ELECTRONICS
- NEW_TECHNOLOGIES
- OFFICE_SUPPORT
- TESTING_QUALITY
- SYSTEM_RESOURCES
- STUDIES_DEVELOPMENT
- SYSTEMS_INFRASTRUCTURE
- OTHER

### Languages:
- FRENCH
- ENGLISH
- ARABIC
- SPANISH
- GERMAN
- ITALIAN
- PORTUGUESE
- CHINESE
- JAPANESE
- OTHER

### Legal Forms (for Companies):
- SARL
- SAS
- PAS
- SUARL
- OTHER

## 🎯 Testing Workflow

### Complete Test Flow:
1. ✅ **Register Freelancer** → Saves token automatically
2. ✅ **Test Freelancer Access** → Uses saved token
3. ✅ **Register Company** → Saves company token
4. ✅ **Test Company Access** → Uses company token
5. ✅ **Login Freelancer** → Test login with existing user
6. ✅ **Test Protected Routes** → Verify JWT works

### MongoDB Verification:
After each registration:
1. Open MongoDB Compass
2. Refresh your database
3. Check the respective collection (freelancers/companies/admins)
4. You should see the new document with all fields

## 🔍 Troubleshooting

### If registration fails:
- Check if email already exists in database
- Verify all required fields are present
- Check password meets minimum 8 characters requirement
- Verify date format is YYYY-MM-DD

### If login fails:
- Verify user exists in database
- Check password is correct
- Ensure email format is valid

### If protected routes fail (401):
- Check token is present in Authorization header
- Verify token format: `Bearer YOUR_TOKEN`
- Token expires after 24 hours - get a new one via login

## 📦 Files Included

- `register-freelancer.json` - Sample freelancer registration
- `register-company.json` - Sample company registration
- `register-admin.json` - Sample admin registration
- `login-freelancer.json` - Freelancer login credentials
- `login-company.json` - Company login credentials
- `login-admin.json` - Admin login credentials
- `WorkLink-Postman-Collection.json` - Complete Postman collection

## 🎉 Success!

Your WorkLink API is working perfectly! All endpoints are tested and ready for development.
