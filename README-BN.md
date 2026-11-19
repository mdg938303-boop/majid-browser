# Majid Browser v1.0

এটি একটি Android Native Kotlin + Android WebView browser starter project।

## ফিচার
- Google search / URL navigation
- একাধিক WebView tab (basic tab switcher)
- Back / Forward / Refresh / Home
- Local bookmarks এবং browsing history
- DownloadManager integration
- Basic incognito mode
- Desktop user-agent toggle
- Share page, clear local history/cache

## Build requirements
- Android Studio
- JDK 17
- Android SDK Platform 35
- Gradle 8.9 (Android Gradle Plugin 8.7.3)

## Build
1. ZIP extract করুন।
2. Android Studio থেকে `MajidBrowser` folder Open করুন।
3. Gradle sync সম্পন্ন হতে দিন।
4. Build > Build Bundle(s) / APK(s) > Build APK(s) নির্বাচন করুন।
5. APK সাধারণত `app/build/outputs/apk/debug/app-debug.apk` এ পাওয়া যাবে।

## গুরুত্বপূর্ণ সীমাবদ্ধতা
- এটি Chrome/Chromium নয়; Android System WebView ব্যবহার করে।
- Incognito এখানে basic: আলাদা WebView, no-cache এবং cookie acceptance বন্ধ করা হয়। এটি সম্পূর্ণ forensic privacy guarantee নয়।
- DownloadManager-এর ফাইল Android Downloads-এ যায়; Android-এর system UI অনুমতি/সেটিংস প্রযোজ্য।
- Android WebView ও Android সংস্করণ অনুযায়ী কিছু সাইটের আচরণ ভিন্ন হতে পারে।
- Production release-এর আগে আরও পরীক্ষা, privacy policy, secure browsing hardening এবং proper app icon assets যোগ করা উচিত।

## Package
`com.majid.browser`

## App version
`1.0 (1)`

## ফোন থেকে ফ্রি APK build (GitHub Actions)
এই ZIP-এ `.github/workflows/android.yml` workflow আছে।
1. GitHub-এ একটি নতুন repository তৈরি করুন।
2. ZIP extract করে সব project file repository-তে upload করুন (root-এ `settings.gradle.kts` থাকবে)।
3. `Actions` tab খুলে `Build Majid Browser APK` workflow নির্বাচন করুন।
4. `Run workflow` চাপুন।
5. Build শেষ হলে workflow run-এর `Artifacts` অংশ থেকে `Majid-Browser-debug-apk` ডাউনলোড করুন।
প্রথমবার GitHub Actions ব্যবহারে repository-তে Actions অনুমোদন করতে হতে পারে।
