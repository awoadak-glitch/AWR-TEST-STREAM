# AWR Stream Hub — Global Fullstack Pro

نسخة كاملة مبدئية تجمع:

- Android Jetpack Compose frontend بتصميم سينمائي.
- تبويبات Home / Movies / K-Drama / Anime / Search / Requests.
- زر Fetch AR + EN SRT داخل تفاصيل كل عمل.
- Request Center لطلب فيلم/مسلسل/أنمي أو ترجمة فوراً.
- Backend Node/Express جاهز محلياً.
- GitHub Actions:
  - بناء APK.
  - جلب 100 عنصر من كل نوع كل 10 دقائق.
  - معالجة الطلبات ذات الأولوية.
  - إنشاء ملفات SRT عند الطلب.
- ملفات JSON داخل `data/` ليقرأها التطبيق لاحقاً من GitHub Raw أو CDN.

## مهم قانونياً

هذا المشروع مصمم لجلب بيانات metadata والترجمات للمحتوى الذي تملك حق استخدامه أو المحتوى المرخص. لا يحتوي على أي نظام لسحب أفلام أو حلقات مقرصنة.

## بناء APK

ارفع المشروع إلى GitHub ثم شغّل workflow:

```bash
gradle :app:assembleDebug --stacktrace
```

## تشغيل backend محلياً

```bash
cd backend
npm install
npm start
```

Endpoints:

```text
GET  /health
GET  /catalog/movies
GET  /catalog/kdrama
GET  /catalog/anime
GET  /search?q=anime
POST /request
POST /subtitle/request
```

## Secrets المقترحة لاحقاً

```text
TMDB_API_KEY
OPENROUTER_API_KEY
GROQ_API_KEY
```

## المرحلة القادمة

ربط التطبيق فعلياً بـ GitHub JSON/API بدل البيانات التجريبية الموجودة داخل الواجهة، ثم إضافة caching وصور حقيقية وإشعارات الطلبات.
