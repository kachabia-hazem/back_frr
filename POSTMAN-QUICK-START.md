# 🚀 Postman Quick Start Guide

## 📥 Step 1: Import Collection

1. **Open Postman** (Download from https://www.postman.com if you don't have it)

2. **Click Import Button**
   - Location: Top left corner of Postman
   - Or use shortcut: `Ctrl + O` (Windows) / `Cmd + O` (Mac)

3. **Select File**
   - Navigate to: `C:\Users\hazem\Desktop\PFE f\workLink\`
   - Select file: `WorkLink-API-Complete.postman_collection.json`
   - Click **Open**

4. **Verify Import**
   - You should see "WorkLink API - Complete Collection" in your left sidebar
   - Expand it to see 4 folders with 10+ endpoints

✅ **Import Complete!**

---

## 🎯 Step 2: Quick Test (5 Minutes)

### Test Flow:

#### 1️⃣ **Register a Freelancer**
```
📁 1. Authentication → Register Freelancer
```
- **Action:** Click "Send"
- **What happens:**
  - User gets created in MongoDB
  - JWT token is automatically saved
  - You see success response
- **Console output:** ✅ "Freelancer registered successfully!"

#### 2️⃣ **Test Protected Route**
```
📁 2. Protected Routes → Test Freelancer Access
```
- **Action:** Click "Send"
- **What happens:**
  - Uses the saved token automatically
  - Verifies JWT authentication works
- **Expected response:** "Accès Freelancer autorisé"

#### 3️⃣ **Login Test**
```
📁 1. Authentication → Login (Any User)
```
- **Email:** ahmed.benali@example.com
- **Password:** Password123!
- **Action:** Click "Send"
- **Result:** New token generated and saved

---

## 📂 Collection Structure

```
WorkLink API - Complete Collection
│
├── 1. Authentication (4 endpoints)
│   ├── Register Freelancer
│   ├── Register Company
│   ├── Register Admin
│   └── Login (Any User)
│
├── 2. Protected Routes (3 endpoints)
│   ├── Test Freelancer Access
│   ├── Test Company Access
│   └── Test Admin Access
│
├── 3. Additional Test Users (3 endpoints)
│   ├── Register Freelancer - Frontend Dev
│   ├── Register Company - Startup
│   └── Register Freelancer - Data Scientist
│
└── 4. Reference - Enum Values
    └── 📖 Enum Values Reference
```

---

## 🔑 Features

### ✨ Automatic Token Management
- **No manual work needed!**
- Tokens are saved automatically after register/login
- Protected routes use saved tokens automatically
- No copy/paste required

### 📊 Test Scripts
- **Automatic validation** on every request
- **Console logs** show what's happening
- **Status checks** verify success/failure
- **Helpful error messages** if something goes wrong

### 📝 Pre-filled Data
- All requests have realistic test data
- Ready to send immediately
- Easy to modify for your needs

---

## 🎮 How to Use

### Using the Collection:

1. **Start with Authentication folder**
   - Register users or login

2. **Tokens are saved automatically**
   - No action needed from you!

3. **Test protected routes**
   - They automatically use the saved tokens

4. **Check Console**
   - View → Show Postman Console (or Alt+Ctrl+C)
   - See detailed logs of what's happening

### Viewing Variables:

**To see saved tokens:**
1. Click on collection name
2. Go to "Variables" tab
3. See all saved tokens and IDs

**Available variables:**
- `baseUrl` - API base URL (http://localhost:8080)
- `freelancer_token` - JWT for freelancer
- `company_token` - JWT for company
- `admin_token` - JWT for admin
- Plus IDs and emails for each user type

---

## 📖 Example Request Flow

### Complete Test Scenario:

```
1. Register Freelancer
   ↓
   ✅ Token saved automatically as {{freelancer_token}}
   ↓
2. Test Freelancer Access (Protected)
   ↓
   ✅ Uses {{freelancer_token}} automatically
   ↓
3. Register Company
   ↓
   ✅ Token saved as {{company_token}}
   ↓
4. Test Company Access (Protected)
   ↓
   ✅ Uses {{company_token}} automatically
   ↓
5. Login with Freelancer Credentials
   ↓
   ✅ New token saved, replaces old one
   ↓
6. Test Freelancer Access Again
   ↓
   ✅ Works with new token
```

---

## 🔍 Checking Results in MongoDB Compass

### After Registration:

1. **Open MongoDB Compass**
2. **Connect to:** mongodb://localhost:27017
3. **Find your database** (probably named "worklink" or "test")
4. **Check collections:**
   - `freelancers` - See Ahmed Ben Ali
   - `companies` - See TechSolutions Tunisia
   - `admins` - See Mohamed Trabelsi

### What to Look For:

```json
{
  "_id": "...",
  "email": "ahmed.benali@example.com",
  "password": "$2a$10$...",  // ✅ Should be hashed!
  "role": "FREELANCER",
  "firstName": "Ahmed",
  "lastName": "Ben Ali",
  // ... more fields
}
```

**Important:** Password should be hashed (starts with `$2a$10$`), not plain text!

---

## 🎨 Postman Console

### Enable Console:
- Windows/Linux: `Ctrl + Alt + C`
- Mac: `Cmd + Alt + C`
- Or: View → Show Postman Console

### What You'll See:
```
🚀 Sending request to: http://localhost:8080/api/auth/register/freelancer
📊 Response status: 201 Created
✅ Freelancer registered successfully!
📧 Email: ahmed.benali@example.com
🔑 Token saved to collection variables
```

---

## ⚡ Pro Tips

### 1. **Run Entire Folder**
- Right-click on "1. Authentication" folder
- Select "Run folder"
- All requests run in sequence

### 2. **Save Responses**
- Click "Save Response" under any request
- Access from "Examples" tab later
- Great for documentation

### 3. **Duplicate & Modify**
- Right-click any request → Duplicate
- Modify data for different test cases
- Create your own test scenarios

### 4. **Environment Variables** (Optional)
- Create different environments (Dev, Staging, Prod)
- Switch baseUrl easily
- Keep tokens separate per environment

---

## 🐛 Troubleshooting

### Issue: "Could not send request"
**Solution:** Make sure your Spring Boot app is running!
```bash
# Check if server is up
curl http://localhost:8080
```

### Issue: 401 Unauthorized on Protected Routes
**Solution:**
1. Token might be missing - Run register/login first
2. Check Variables tab to see if token is saved
3. Token expired (24 hours) - Login again

### Issue: "Email already exists"
**Solution:**
1. User already registered - Use login instead
2. Or change email in request body
3. Or delete user from MongoDB and try again

### Issue: Can't see requests in collection
**Solution:**
1. Make sure you imported the JSON file
2. Refresh Postman (Ctrl+R)
3. Check left sidebar - expand collection

---

## 📚 Available Test Data

### Pre-configured Users:

| Type | Name | Email | Password |
|------|------|-------|----------|
| Freelancer | Ahmed Ben Ali | ahmed.benali@example.com | Password123! |
| Company | TechSolutions | contact@techsolutions.tn | CompanyPass123! |
| Admin | Mohamed Trabelsi | mohamed.trabelsi@worklink.com | AdminPass123! |
| Freelancer | Sarah Jebali | sarah.jebali@example.com | Password123! |
| Company | InnovateTech | contact@innovatetech.tn | CompanyPass456! |
| Freelancer | Youssef Hamdi | youssef.hamdi@example.com | Password123! |

---

## ✅ Success Checklist

After importing, you should be able to:

- [ ] See collection in left sidebar with 4 folders
- [ ] Send "Register Freelancer" successfully
- [ ] See token saved in Variables tab
- [ ] Send "Test Freelancer Access" successfully
- [ ] See user in MongoDB Compass
- [ ] Login with registered user
- [ ] Get new token after login
- [ ] Test all 3 user types (Freelancer, Company, Admin)

---

## 🎉 You're Ready!

Your Postman collection is fully configured and ready to test your WorkLink API!

**Next Steps:**
1. Import the collection ✅
2. Run "Register Freelancer" ✅
3. Check MongoDB Compass ✅
4. Test protected routes ✅
5. Build more features! 🚀

**Need Help?**
- Check Postman Console for detailed logs
- Review response bodies for error messages
- Verify server is running on http://localhost:8080

---

**Happy Testing! 🎯**
