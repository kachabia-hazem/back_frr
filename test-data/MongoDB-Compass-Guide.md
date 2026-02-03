# MongoDB Compass - Viewing Your Data

## 🔌 Connection Steps

1. **Open MongoDB Compass**
2. **Connection String**: `mongodb://localhost:27017`
3. **Click Connect**

## 📂 Database Structure

Your application uses these collections:

```
worklink (or your database name)
├── freelancers
├── companies
└── admins
```

## 👤 Freelancer Collection

**Collection Name**: `freelancers`

**What you should see**:
```json
{
  "_id": "697b85f877a8b173eb4c185e",
  "firstName": "Ahmed",
  "lastName": "Ben Ali",
  "email": "ahmed.benali@example.com",
  "password": "$2a$10$...", // Hashed password
  "gender": "MALE",
  "dateOfBirth": "1995-05-15T00:00:00.000Z",
  "phoneNumber": "+21612345678",
  "yearsOfExperience": 5,
  "profileTypes": ["STUDIES_DEVELOPMENT", "NEW_TECHNOLOGIES"],
  "tjm": 450.0,
  "languages": ["FRENCH", "ENGLISH", "ARABIC"],
  "currentPosition": "Senior Full Stack Developer",
  "bio": "Experienced full-stack developer specialized in Spring Boot and Angular",
  "skills": ["Java", "Spring Boot", "Angular", "MongoDB", "Docker"],
  "portfolioUrl": "https://github.com/ahmedbenali",
  "role": "FREELANCER",
  "enabled": true,
  "createdAt": "2026-01-29T16:08:24.XXX",
  "_class": "com.hazem.worklink.models.Freelancer"
}
```

**Key Fields to Check**:
- ✅ `email`: ahmed.benali@example.com
- ✅ `password`: Should be hashed (starts with $2a$)
- ✅ `role`: FREELANCER
- ✅ `profileTypes`: Array with multiple values
- ✅ `skills`: Array of technologies
- ✅ `tjm`: Daily rate (450.0)

## 🏢 Company Collection

**Collection Name**: `companies`

**What you should see**:
```json
{
  "_id": "697b860777a8b173eb4c185f",
  "companyName": "TechSolutions Tunisia",
  "email": "contact@techsolutions.tn",
  "password": "$2a$10$...", // Hashed password
  "address": "Avenue Habib Bourguiba, Tunis 1000, Tunisia",
  "websiteUrl": "https://www.techsolutions.tn",
  "legalForm": "SARL",
  "tradeRegister": "B123456789",
  "foundationDate": "2015-03-20T00:00:00.000Z",
  "businessSector": "Information Technology",
  "managerName": "Fatma Karoui",
  "managerEmail": "fatma.karoui@techsolutions.tn",
  "managerPosition": "CEO",
  "managerPhoneNumber": "+21698765432",
  "description": "Leading IT consulting company in Tunisia",
  "numberOfEmployees": 50,
  "role": "COMPANY",
  "enabled": true,
  "createdAt": "2026-01-29T16:08:39.XXX",
  "_class": "com.hazem.worklink.models.Company"
}
```

**Key Fields to Check**:
- ✅ `email`: contact@techsolutions.tn
- ✅ `companyName`: TechSolutions Tunisia
- ✅ `role`: COMPANY
- ✅ `legalForm`: SARL
- ✅ `tradeRegister`: B123456789
- ✅ `managerName`: Fatma Karoui
- ✅ `numberOfEmployees`: 50

## 👨‍💼 Admin Collection

**Collection Name**: `admins`

**What you should see**:
```json
{
  "_id": "697b860f77a8b173eb4c1860",
  "firstName": "Mohamed",
  "lastName": "Trabelsi",
  "email": "mohamed.trabelsi@worklink.com",
  "password": "$2a$10$...", // Hashed password
  "gender": "MALE",
  "dateOfBirth": "1988-11-10T00:00:00.000Z",
  "phoneNumber": "+21655443322",
  "currentPosition": "System Administrator",
  "yearsOfExperience": 8,
  "department": "IT Administration",
  "role": "ADMIN",
  "enabled": true,
  "createdAt": "2026-01-29T16:08:47.XXX",
  "_class": "com.hazem.worklink.models.Admin"
}
```

**Key Fields to Check**:
- ✅ `email`: mohamed.trabelsi@worklink.com
- ✅ `role`: ADMIN
- ✅ `department`: IT Administration
- ✅ `currentPosition`: System Administrator
- ✅ `yearsOfExperience`: 8

## 🔍 How to Find Documents in Compass

### Method 1: Browse All Documents
1. Click on database name (left sidebar)
2. Click on collection name (e.g., `freelancers`)
3. Click **Documents** tab
4. You'll see all documents listed

### Method 2: Search by Email
1. Go to collection
2. Click **Filter** field
3. Enter: `{"email": "ahmed.benali@example.com"}`
4. Press Enter

### Method 3: Search by Role
1. Go to collection
2. Filter: `{"role": "FREELANCER"}`
3. See all freelancers

## ✅ Verification Checklist

Use this checklist to verify your data:

### Freelancer (Ahmed Ben Ali)
- [ ] Email: ahmed.benali@example.com exists
- [ ] Password is hashed (not plain text)
- [ ] Role is FREELANCER
- [ ] Has 5 skills in array
- [ ] Has 2 profile types
- [ ] Has 3 languages
- [ ] TJM is 450.0
- [ ] Years of experience is 5

### Company (TechSolutions)
- [ ] Email: contact@techsolutions.tn exists
- [ ] Company name: TechSolutions Tunisia
- [ ] Role is COMPANY
- [ ] Legal form is SARL
- [ ] Has manager details (Fatma Karoui)
- [ ] Number of employees is 50
- [ ] Has website URL

### Admin (Mohamed Trabelsi)
- [ ] Email: mohamed.trabelsi@worklink.com exists
- [ ] Role is ADMIN
- [ ] Department: IT Administration
- [ ] Years of experience is 8
- [ ] Phone number starts with +216

## 🎨 Compass Tips

### View as JSON
- Click the **{}** icon to switch between table and JSON view
- JSON view shows the complete document structure

### Export Data
1. Click **Export Data** button
2. Choose JSON or CSV format
3. Save for backup or analysis

### Filter Examples
```javascript
// Find all freelancers
{"role": "FREELANCER"}

// Find users created today
{"createdAt": {"$gte": ISODate("2026-01-29T00:00:00Z")}}

// Find freelancers with specific skill
{"skills": "Java"}

// Find companies with more than 20 employees
{"numberOfEmployees": {"$gt": 20}}

// Find by ID
{"_id": ObjectId("697b85f877a8b173eb4c185e")}
```

## 📊 Expected Document Count

After running all test registrations:
- **Freelancers**: 1 document (Ahmed)
- **Companies**: 1 document (TechSolutions)
- **Admins**: 1 document (Mohamed)

**Total**: 3 users registered successfully!

## 🔐 Password Security Check

**Important**: In MongoDB Compass, you should see:
```json
"password": "$2a$10$SomeHashedString..."
```

**NOT**:
```json
"password": "Password123!"
```

If you see plain text passwords, there's a security issue!

## 🚀 Next Steps

After verifying data in Compass:
1. ✅ Test login with these credentials in Postman
2. ✅ Test protected routes with JWT tokens
3. ✅ Try registering new users
4. ✅ Verify duplicate email prevention
5. ✅ Test invalid credentials

## 📸 Screenshots to Take

For documentation, capture:
1. Database overview showing 3 collections
2. Freelancer document in JSON view
3. Company document showing all fields
4. Admin document
5. Filter results showing all roles

---

**Status**: ✅ All data should be visible in MongoDB Compass now!
