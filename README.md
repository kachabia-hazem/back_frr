# WorkLink API - Backend

Platform de freelance - API Spring Boot avec MongoDB.

## Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB 5.0+

## Configuration

### 1. Copier le fichier d'environnement

```bash
cp .env.example .env
```

### 2. Configurer les variables dans `.env`

Ouvrir le fichier `.env` et remplir les valeurs:

```properties
# JWT - Générer avec: openssl rand -base64 64
JWT_SECRET=votre-secret-jwt

# Google OAuth (https://console.cloud.google.com/)
GOOGLE_CLIENT_ID=votre-google-client-id

# LinkedIn OAuth (https://www.linkedin.com/developers/)
LINKEDIN_CLIENT_ID=votre-linkedin-client-id
LINKEDIN_CLIENT_SECRET=votre-linkedin-client-secret

# Gmail SMTP (https://myaccount.google.com/apppasswords)
MAIL_USERNAME=votre-email@gmail.com
MAIL_PASSWORD=votre-app-password
```

## Lancer l'application

```bash
# Installer les dépendances et lancer
mvn spring-boot:run

# Ou compiler et lancer
mvn clean package
java -jar target/workLink-0.0.1-SNAPSHOT.jar
```

L'API sera disponible sur `http://localhost:8080`

## API Endpoints

- `POST /api/auth/register` - Inscription
- `POST /api/auth/login` - Connexion
- `GET /api/freelancer/me` - Profil freelancer
- `PUT /api/freelancer/me` - Mise à jour profil
- `POST /api/files/profile-pictures` - Upload photo
- `POST /api/files/cvs` - Upload CV

## Structure

```
src/main/java/com/hazem/worklink/
├── config/          # Configuration (Security, etc.)
├── controllers/     # REST Controllers
├── dto/             # Data Transfer Objects
├── models/          # MongoDB Documents
├── repositories/    # MongoDB Repositories
├── security/        # JWT & Auth
└── services/        # Business Logic
```
