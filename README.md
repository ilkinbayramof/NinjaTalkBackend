# NinjaTalk Backend API

NinjaTalk anonim sohbet uygulaması için backend API. Kotlin ve Ktor framework kullanılarak geliştirilmiştir.

## 🚀 Özellikler

- ✅ Kullanıcı kaydı (Register)
- ✅ Kullanıcı girişi (Login)
- ✅ JWT token tabanlı authentication
- ✅ BCrypt ile şifre hashleme
- ✅ MongoDB veritabanı
- ✅ CORS desteği
- ✅ Error handling

## 📋 Gereksinimler

- JDK 17+
- MongoDB Atlas hesabı (ücretsiz)
- Gradle

## 🛠️ Kurulum

### 1. MongoDB Atlas Kurulumu

1. [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register) hesabı oluşturun
2. Yeni bir **FREE** cluster oluşturun (M0 Sandbox)
3. Database Access'ten yeni bir kullanıcı oluşturun
4. Network Access'ten IP adresinizi ekleyin (veya `0.0.0.0/0` ile herkese açın)
5. Connect butonuna tıklayıp connection string'i kopyalayın

### 2. Environment Variables

`.env.example` dosyasını `.env` olarak kopyalayın ve doldurun:

\`\`\`bash
cp .env.example .env
\`\`\`

`.env` dosyasını düzenleyin:

\`\`\`
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/ninjatalk?retryWrites=true&w=majority
JWT_SECRET=your-super-secret-jwt-key-change-this
PORT=8080
\`\`\`

### 3. Local Çalıştırma

\`\`\`bash
./gradlew run
\`\`\`

Server `http://localhost:8080` adresinde çalışacaktır.

## 📡 API Endpoints

### Health Check
\`\`\`
GET /health
\`\`\`

### Register
\`\`\`
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "gender": "MALE",
  "birthDate": "2000-01-15"
}
\`\`\`

**Response (201 Created):**
\`\`\`json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com"
}
\`\`\`

### Login
\`\`\`
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
\`\`\`

**Response (200 OK):**
\`\`\`json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com"
}
\`\`\`

## 🌐 Deployment (Railway - ÖNERİLEN)

### Railway.app ile Ücretsiz Deploy

1. [Railway.app](https://railway.app/) hesabı oluşturun (GitHub ile giriş yapın)

2. Yeni proje oluşturun: **Deploy from GitHub repo**

3. Repository'nizi seçin

4. Environment Variables ekleyin:
   - `MONGODB_URI`: MongoDB Atlas connection string
   - `JWT_SECRET`: Güvenli bir random string
   - `PORT`: Railway otomatik atayacak (boş bırakabilirsiniz)

5. Deploy! 🎉

Railway otomatik olarak:
- Dockerfile'ı kullanarak build edecek
- HTTPS sağlayacak
- Otomatik domain verecek (örn: `ninjatalk-production.up.railway.app`)

### Alternatif: Render.com

1. [Render.com](https://render.com/) hesabı oluşturun
2. New Web Service → Connect GitHub repo
3. Environment variables ekleyin
4. Deploy!

⚠️ **Not:** Render free tier 15 dakika inaktiviteden sonra uyur.

## 🧪 Test

### cURL ile Test

**Register:**
\`\`\`bash
curl -X POST http://localhost:8080/api/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "test@example.com",
    "password": "test123",
    "gender": "MALE",
    "birthDate": "2000-01-15"
  }'
\`\`\`

**Login:**
\`\`\`bash
curl -X POST http://localhost:8080/api/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "test@example.com",
    "password": "test123"
  }'
\`\`\`

## 📱 Android Entegrasyonu

Android uygulamanızda Retrofit kullanarak bağlanabilirsiniz:

\`\`\`kotlin
// Base URL'i deployment URL'iniz ile değiştirin
const val BASE_URL = "https://your-app.up.railway.app/"

// veya local test için
const val BASE_URL = "http://10.0.2.2:8080/" // Android emulator
\`\`\`

## 🔒 Güvenlik

- ✅ Şifreler BCrypt ile hashlenmiş
- ✅ JWT token 24 saat geçerli
- ✅ CORS yapılandırılmış
- ⚠️ Production'da `anyHost()` yerine spesifik domain kullanın

## 📝 Lisans

MIT
