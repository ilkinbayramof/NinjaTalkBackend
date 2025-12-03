# 🎯 NinjaTalk Backend Deployment Rehberi

## Adım 1: MongoDB Atlas Kurulumu (5 dakika)

### 1.1 Hesap Oluşturma
1. [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register)'a gidin
2. Google veya email ile ücretsiz hesap oluşturun
3. "Build a Database" butonuna tıklayın

### 1.2 Cluster Oluşturma
1. **FREE** tier seçin (M0 Sandbox)
2. Provider: **AWS** (veya Google Cloud)
3. Region: Size en yakın bölge (örn: Frankfurt)
4. Cluster Name: `NinjaTalk` (veya istediğiniz isim)
5. "Create" butonuna tıklayın (2-3 dakika sürer)

### 1.3 Database Kullanıcısı Oluşturma
1. Sol menüden **Database Access** seçin
2. "Add New Database User" butonuna tıklayın
3. Authentication Method: **Password**
4. Username: `ninjatalk` (veya istediğiniz)
5. Password: Güvenli bir şifre oluşturun (kaydedin!)
6. Database User Privileges: **Read and write to any database**
7. "Add User" butonuna tıklayın

### 1.4 Network Access Ayarları
1. Sol menüden **Network Access** seçin
2. "Add IP Address" butonuna tıklayın
3. "Allow Access from Anywhere" seçin (0.0.0.0/0)
4. "Confirm" butonuna tıklayın

### 1.5 Connection String Alma
1. Sol menüden **Database** seçin
2. Cluster'ınızın yanındaki "Connect" butonuna tıklayın
3. "Drivers" seçin
4. Driver: **Node.js** (veya herhangi biri)
5. Connection string'i kopyalayın:
   ```
   mongodb+srv://ninjatalk:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
6. `<password>` kısmını gerçek şifrenizle değiştirin

✅ MongoDB Atlas hazır!

---

## Adım 2: Railway.app ile Deployment (10 dakika)

### 2.1 Railway Hesabı
1. [Railway.app](https://railway.app/)'e gidin
2. "Login" → **"Login with GitHub"** ile giriş yapın
3. GitHub hesabınızı bağlayın

### 2.2 Proje Oluşturma
1. Dashboard'da "New Project" butonuna tıklayın
2. "Deploy from GitHub repo" seçin
3. Repository'nizi seçin: `NinjaTalkBackEnd`
4. Railway otomatik olarak Dockerfile'ı algılayacak

### 2.3 Environment Variables Ekleme
1. Projenizin sayfasında **"Variables"** tab'ına gidin
2. Şu değişkenleri ekleyin:

   **MONGODB_URI:**
   ```
   mongodb+srv://ninjatalk:yourpassword@cluster0.xxxxx.mongodb.net/ninjatalk?retryWrites=true&w=majority
   ```
   (MongoDB Atlas'tan aldığınız connection string)

   **JWT_SECRET:**
   ```
   super-secret-jwt-key-2024-ninjatalk-production
   ```
   (Güvenli bir random string)

3. "Add" butonuna tıklayın

### 2.4 Deploy
1. Railway otomatik olarak deploy edecek
2. "Deployments" tab'ında ilerlemeyi izleyin
3. Build tamamlandığında (2-3 dakika) ✅ yeşil işaret göreceksiniz

### 2.5 Public URL Alma
1. "Settings" tab'ına gidin
2. "Networking" bölümünde "Generate Domain" butonuna tıklayın
3. Otomatik domain oluşturulacak:
   ```
   https://ninjatalkbackend-production.up.railway.app
   ```
4. Bu URL'i kopyalayın - Android uygulamanızda kullanacaksınız!

✅ Backend deploy edildi ve çalışıyor!

---

## Adım 3: Test Etme

### 3.1 Health Check
Tarayıcınızda şu URL'i açın:
```
https://your-app.up.railway.app/health
```
"OK" yazısını görmelisiniz.

### 3.2 Register Test
Terminal'de:
```bash
curl -X POST https://your-app.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123",
    "gender": "MALE",
    "birthDate": "2000-01-15"
  }'
```

Başarılı yanıt:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "...",
  "email": "test@example.com"
}
```

### 3.3 Login Test
```bash
curl -X POST https://your-app.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123"
  }'
```

✅ Her iki test de başarılı olmalı!

---

## Adım 4: Android Uygulamasına Bağlama

### 4.1 Base URL Güncelleme
Android projenizde API base URL'ini güncelleyin:

```kotlin
// Önceki (local):
const val BASE_URL = "http://10.0.2.2:8080/"

// Yeni (production):
const val BASE_URL = "https://your-app.up.railway.app/"
```

### 4.2 Internet Permission
`AndroidManifest.xml` dosyasında internet izni olduğundan emin olun:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 4.3 Test
1. Android uygulamanızı çalıştırın
2. Register ekranından yeni kullanıcı oluşturun
3. Login yapın
4. Ana ekrana yönlendirilmelisiniz!

---

## 🎉 Tamamlandı!

Backend'iniz artık ücretsiz olarak çalışıyor:
- ✅ MongoDB Atlas (512 MB ücretsiz)
- ✅ Railway.app (500 saat/ay ücretsiz)
- ✅ HTTPS otomatik
- ✅ Otomatik deploy (GitHub'a push yapınca)

## 📊 Monitoring

Railway Dashboard'dan:
- CPU/RAM kullanımı
- Request sayısı
- Loglar
- Deploy geçmişi

hepsini görebilirsiniz.

## 🔄 Güncelleme

Kod değişikliği yaptığınızda:
1. GitHub'a push yapın
2. Railway otomatik olarak yeniden deploy eder
3. 2-3 dakika sonra yeni versiyon yayında!

## ⚠️ Önemli Notlar

- Railway free tier: **500 saat/ay** (yaklaşık 20 gün 24/7)
- Eğer limit dolursa: Render.com'a geçin (tamamen ücretsiz ama uyur)
- MongoDB Atlas: **512 MB** yeterli (binlerce kullanıcı)
- Production'da CORS ayarlarını güncellemeyi unutmayın!
