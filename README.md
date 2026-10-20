# رشد مالی | Financial Growth

اپلیکیشن بومی اندروید برای مدیریت مالی شخصی. این مخزن در حال حاضر اسکلت اولیه برنامه را شامل می‌شود؛ قابلیت‌های ثبت و تحلیل مالی هنوز پیاده‌سازی نشده‌اند.

## فناوری‌ها

- Kotlin و Jetpack Compose
- Android Gradle Plugin 8.13.2، Kotlin 2.4.0 و Gradle 8.14.4
- پایگاه داده محلی Room در مرحله بعد اضافه خواهد شد.
- برنامه برای نمایش صفحه آغازین به اینترنت یا حساب کاربری نیاز ندارد.

## پیش‌نیازها

- JDK 17 یا بالاتر؛ در این محیط توسعه از JDK 21 استفاده شده است.
- Android SDK، شامل Android SDK Platform 36 و ابزارهای ساخت مربوطه.

## ساخت و اجرا

پس از نصب Android SDK و تنظیم مسیر آن در `local.properties` یا متغیر `ANDROID_HOME`، برای ساخت نسخه اشکال‌زدایی اجرا کنید:

```bash
./gradlew :app:assembleDebug
```

APK در مسیر `app/build/outputs/apk/debug/app-debug.apk` ساخته می‌شود. اجرای آزمون رابط کاربری به دستگاه یا شبیه‌ساز اندروید نیاز دارد:

```bash
./gradlew :app:connectedDebugAndroidTest
```

شناسه بسته فعلی `com.roshdmali.app` موقت است و باید پیش از انتشار نهایی تأیید شود.
