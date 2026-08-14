# Build the APK on GitHub (no Android Studio needed)

1. Create a new GitHub repository, for example `NoorasWatermark`.
2. Upload all files/folders from this project to the repository root.
3. Make sure `.github/workflows/build-apk.yml` is included.
4. Commit the files to the `main` branch.
5. Open the repository's **Actions** tab.
6. Select **Build Nooras Watermark APK**.
7. Click **Run workflow** (or push to `main` and let it run automatically).
8. Open the completed workflow run.
9. Scroll to **Artifacts** and download **Nooras-Watermark-debug-APK**.
10. Extract the downloaded artifact and install `app-debug.apk` on the Android phone.

The app processes photos locally on the phone. The GitHub workflow is only used to compile the APK.
