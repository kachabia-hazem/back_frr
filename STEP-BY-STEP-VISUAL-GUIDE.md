# 📸 Visual Step-by-Step Guide

## 🎯 Import & Test in Postman (3 Minutes)

---

## ✅ STEP 1: Import Collection

### 1.1 Open Postman

```
[Open Postman Application]
```
- If you don't have Postman, download from: https://www.postman.com/downloads/

### 1.2 Click Import Button

```
┌─────────────────────────────────────────────┐
│ Postman                             [− □ ×] │
├─────────────────────────────────────────────┤
│  [Import ▼]  New ▼   📁 Collections         │
│  ↑                                          │
│  Click this button                          │
└─────────────────────────────────────────────┘
```
- Location: **Top left corner**
- Button text: "Import"

### 1.3 Select File

```
File Location:
C:\Users\hazem\Desktop\PFE f\workLink\WorkLink-API-Complete.postman_collection.json
```

**What to do:**
1. In import dialog, click "Choose Files" or drag & drop
2. Navigate to the file location above
3. Select `WorkLink-API-Complete.postman_collection.json`
4. Click "Open"
5. Click "Import" button

### 1.4 Verify Import Success

**You should now see:**
```
Collections (left sidebar)
├── 📁 WorkLink API - Complete Collection
    ├── 📁 1. Authentication (4)
    ├── 📁 2. Protected Routes (3)
    ├── 📁 3. Additional Test Users (3)
    └── 📁 4. Reference - Enum Values (1)
```

**✅ Import Complete!** If you see this structure, you're ready to go!

---

## ✅ STEP 2: First Request - Register Freelancer

### 2.1 Navigate to Request

**Click path:**
```
Collections
└── WorkLink API - Complete Collection
    └── 1. Authentication
        └── Register Freelancer  ← Click here
```

### 2.2 Review Request

**You should see:**

```
┌─────────────────────────────────────────────────────────────┐
│  POST   http://localhost:8080/api/auth/register/freelancer  │
│  [Params] [Authorization] [Headers] [Body] [Pre-req...      │
├─────────────────────────────────────────────────────────────┤
│  Body tab selected:                                         │
│  ● raw   ▼ JSON                                            │
│                                                             │
│  {                                                          │
│    "firstName": "Ahmed",                                    │
│    "lastName": "Ben Ali",                                   │
│    "email": "ahmed.benali@example.com",                    │
│    "password": "Password123!",                             │
│    ... (more fields)                                        │
│  }                                                          │
│                                                             │
│  [Send]  [Save ▼]                                          │
└─────────────────────────────────────────────────────────────┘
```

**Everything is pre-filled! No need to type anything.**

### 2.3 Send Request

```
┌─────────────────────────┐
│      [Send]  [Save ▼]   │
│       ↑                 │
│   Click here            │
└─────────────────────────┘
```

**Click the blue "Send" button**

### 2.4 View Response

**Expected Response (Status: 201 Created):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiRlJFRUxBTkNFUiI...",
  "email": "ahmed.benali@example.com",
  "role": "FREELANCER",
  "id": "697b85f877a8b173eb4c185e",
  "message": "Freelancer enregistré avec succès"
}
```

**✅ Success Indicators:**
- Status: `201 Created` (green)
- Message: "Freelancer enregistré avec succès"
- Token is present (long string starting with "eyJ...")

### 2.5 Check Console (Optional but Recommended)

**Open Console:**
- Windows/Linux: Press `Ctrl + Alt + C`
- Mac: Press `Cmd + Alt + C`

**You should see:**
```
🚀 Sending request to: http://localhost:8080/api/auth/register/freelancer
📊 Response status: 201 Created
✅ Freelancer registered successfully!
📧 Email: ahmed.benali@example.com
🔑 Token saved to collection variables
```

**✅ Perfect!** Token is automatically saved.

---

## ✅ STEP 3: Test Protected Route

### 3.1 Navigate to Protected Route

**Click path:**
```
Collections
└── WorkLink API - Complete Collection
    └── 2. Protected Routes
        └── Test Freelancer Access  ← Click here
```

### 3.2 Review Authorization

**Check Authorization Tab:**

```
┌─────────────────────────────────────────────────────────────┐
│  GET   http://localhost:8080/api/auth/test/freelancer       │
│  [Params] [Authorization] [Headers] [Body]                  │
│           ↑ Click this tab                                  │
├─────────────────────────────────────────────────────────────┤
│  Type:  Bearer Token                                        │
│  Token: {{freelancer_token}}  ← Uses saved variable!       │
│                                                             │
│  ℹ️  This request will automatically use the token         │
│     that was saved when you registered.                    │
└─────────────────────────────────────────────────────────────┘
```

**No need to copy/paste token!** It uses `{{freelancer_token}}` variable automatically.

### 3.3 Send Request

**Click "Send" button**

### 3.4 View Response

**Expected Response (Status: 200 OK):**

```
Accès Freelancer autorisé
```

**✅ Success!** Your JWT authentication is working!

**Console Output:**
```
🚀 Sending request to: http://localhost:8080/api/auth/test/freelancer
📊 Response status: 200 OK
✅ Freelancer access granted
```

---

## ✅ STEP 4: View in MongoDB Compass

### 4.1 Open MongoDB Compass

```
[Launch MongoDB Compass Application]
```

### 4.2 Connect to Database

**Connection String:**
```
mongodb://localhost:27017
```

**What to do:**
1. Paste connection string in the connection field
2. Click "Connect" button

### 4.3 Find Your Database

**In left sidebar, look for your database:**
```
Databases:
├── admin
├── config
├── local
└── worklink  ← Your database (or similar name like "test")
```

**Click on your database name**

### 4.4 View Collections

**You should see 3 collections:**
```
worklink
├── 📁 freelancers (1 document)
├── 📁 companies (0 documents)
└── 📁 admins (0 documents)
```

### 4.5 View Freelancer Document

**Click on "freelancers" collection**

**You should see Ahmed's data:**
```json
{
  "_id": "697b85f877a8b173eb4c185e",
  "firstName": "Ahmed",
  "lastName": "Ben Ali",
  "email": "ahmed.benali@example.com",
  "password": "$2a$10$abc123...",  // ← Hashed! (GOOD)
  "role": "FREELANCER",
  "gender": "MALE",
  "dateOfBirth": "1995-05-15T00:00:00.000Z",
  "phoneNumber": "+21612345678",
  "yearsOfExperience": 5,
  "profileTypes": ["STUDIES_DEVELOPMENT", "NEW_TECHNOLOGIES"],
  "tjm": 450.0,
  "languages": ["FRENCH", "ENGLISH", "ARABIC"],
  "skills": ["Java", "Spring Boot", "Angular", "MongoDB", "Docker"],
  "currentPosition": "Senior Full Stack Developer",
  "bio": "Experienced full-stack developer...",
  "portfolioUrl": "https://github.com/ahmedbenali",
  "enabled": true,
  "createdAt": "2026-01-29T16:08:24.123Z",
  "_class": "com.hazem.worklink.models.Freelancer"
}
```

**✅ Perfect!** All data is saved correctly!

**🔒 Security Check:**
- Password starts with `$2a$10$` = Hashed ✅
- If you see plain text password = Security issue ❌

---

## ✅ STEP 5: Test Login

### 5.1 Navigate to Login Request

**Click path:**
```
Collections
└── WorkLink API - Complete Collection
    └── 1. Authentication
        └── Login (Any User)  ← Click here
```

### 5.2 Review Request Body

**Pre-filled with freelancer credentials:**
```json
{
  "email": "ahmed.benali@example.com",
  "password": "Password123!"
}
```

### 5.3 Send Request

**Click "Send"**

### 5.4 View Response

**Expected Response (Status: 200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.NEW_TOKEN_HERE...",
  "email": "ahmed.benali@example.com",
  "role": "FREELANCER",
  "id": "697b85f877a8b173eb4c185e",
  "message": "Connexion réussie"
}
```

**Console Output:**
```
✅ Freelancer logged in
📧 Email: ahmed.benali@example.com
👤 Role: FREELANCER
🔑 Token saved
```

**✅ Success!** New token generated and saved!

---

## ✅ STEP 6: View Saved Variables

### 6.1 Access Variables Tab

**Two ways to view variables:**

**Method 1: From Collection**
```
Right-click on "WorkLink API - Complete Collection"
└── Select "Edit"
    └── Click "Variables" tab
```

**Method 2: Quick View**
```
Click collection name in sidebar
└── Variables tab appears in main panel
```

### 6.2 View Saved Variables

**You should see:**

| Variable | Current Value | Type |
|----------|--------------|------|
| baseUrl | http://localhost:8080 | string |
| freelancer_token | eyJhbGciOiJIU... | string |
| company_token | (empty initially) | string |
| admin_token | (empty initially) | string |
| freelancer_id | 697b85f877a8b173eb4c185e | string |
| freelancer_email | ahmed.benali@example.com | string |
| company_id | | string |
| admin_id | | string |

**✅ See the tokens?** That's automatic token management in action!

---

## 🎯 Complete Test Flow (All User Types)

### Run These in Order:

```
1. Register Freelancer ✅
   ↓ (token saved as {{freelancer_token}})

2. Test Freelancer Access ✅
   ↓ (uses {{freelancer_token}})

3. Register Company ✅
   ↓ (token saved as {{company_token}})

4. Test Company Access ✅
   ↓ (uses {{company_token}})

5. Register Admin ✅
   ↓ (token saved as {{admin_token}})

6. Test Admin Access ✅
   ↓ (uses {{admin_token}})
```

**After running all 6 requests:**

**In MongoDB Compass you'll see:**
```
freelancers: 1 document (Ahmed)
companies: 1 document (TechSolutions)
admins: 1 document (Mohamed)
```

**In Postman Variables:**
```
freelancer_token: [JWT token]
company_token: [JWT token]
admin_token: [JWT token]
```

**✅ All user types tested successfully!**

---

## 📊 Status Codes Reference

### Success Responses:
- **200 OK** - Request successful (login, protected routes)
- **201 Created** - Resource created (registration)

### Error Responses:
- **400 Bad Request** - Validation error (missing fields, invalid format)
- **401 Unauthorized** - Missing or invalid token
- **403 Forbidden** - Token valid but wrong role
- **409 Conflict** - Email already exists

---

## 🔍 What If Something Goes Wrong?

### Error: "Could not send request"

**Visual:**
```
┌─────────────────────────────────────┐
│  ⚠️ Error: Could not send request   │
│                                     │
│  Could not connect to server        │
└─────────────────────────────────────┘
```

**Solution:**
1. Check if Spring Boot app is running
2. Look for this in your IDE console:
   ```
   Tomcat started on port 8080 (http)
   Started WorkLinkApplication in X seconds
   ```
3. Test connection: Open browser → http://localhost:8080

---

### Error: "Email already exists"

**Response:**
```json
{
  "token": null,
  "email": null,
  "role": null,
  "id": null,
  "message": "Email already exists"
}
```

**Solution:**
1. Use "Login" instead of "Register"
2. Or change email in request body
3. Or delete user from MongoDB

---

### Error: 401 Unauthorized on Protected Route

**Response:**
```
Status: 401 Unauthorized
```

**Solution:**
1. Check Variables tab - is token saved?
2. If empty, run Register or Login first
3. Token might be expired (24 hours) - login again

---

## ✅ Success Checklist

Mark each as you complete:

- [ ] Imported collection successfully
- [ ] Sent "Register Freelancer" → Got 201 Created
- [ ] Saw token in response
- [ ] Checked Variables tab → Token is saved
- [ ] Sent "Test Freelancer Access" → Got 200 OK
- [ ] Opened MongoDB Compass
- [ ] Connected to localhost:27017
- [ ] Found database (worklink/test)
- [ ] Saw freelancer document with Ahmed's data
- [ ] Password is hashed (starts with $2a$10$)
- [ ] Tested "Login" → Got new token
- [ ] All 3 user types registered successfully

**If you checked all boxes:** 🎉 **YOU'RE A POSTMAN PRO!**

---

## 🎓 Next Steps

### Beginner:
1. ✅ Test all endpoints in "1. Authentication"
2. ✅ Test all endpoints in "2. Protected Routes"
3. ✅ View all users in MongoDB Compass

### Intermediate:
1. Create your own test users with different data
2. Test error cases (invalid email, short password, etc.)
3. Try duplicate email to see error handling

### Advanced:
1. Run entire folders using Runner
2. Create automated test suites
3. Export data from MongoDB
4. Build new endpoints and add to collection

---

## 🎉 Congratulations!

**You now know how to:**
- ✅ Import Postman collections
- ✅ Send API requests
- ✅ View automatic token management
- ✅ Test protected routes
- ✅ Verify data in MongoDB
- ✅ Troubleshoot common issues

**Your API is working perfectly!** 🚀

---

**Need more help?**
- Read: `POSTMAN-QUICK-START.md`
- Read: `test-data/README.md`
- Check Postman Console for detailed logs
- Review response bodies for error messages

**Happy Testing!** 🎯
