# Ball Passage Test

Android app that calculates the steel ball diameter for the **ball passage test**
(ball test, *Kugeldurchlaufversuch*, *kulowanie*) of bent tubes according to
**VGB-S-013-00-2014-12, section 7.2.5, Table 3**.

<p>
  <img src="docs/screenshots/result.png" width="180" alt="Result with ball suggestion">
  <img src="docs/screenshots/kbd.png" width="180" alt="Result above the keyboard">
  <img src="docs/screenshots/balls.png" width="180" alt="Ball set">
  <img src="docs/screenshots/extrapolated.png" width="180" alt="Extrapolated result">
  <img src="docs/screenshots/dark.png" width="180" alt="Dark theme">
</p>

## Features

- **Calculator.** Results update as you type. While the keyboard is open, a compact result bar
  replaces the navigation bar, so the result stays visible on small phones. The screen also
  shows the step-by-step calculation and Table 3 with the cell that was used highlighted.
  When a tube is outside the table, the app says why, for example the minimum bend radius.
- **Ball sets.** Keep the steel balls of each workshop or case as a separate set and switch the
  active one. You can add balls one by one or as a range (e.g. 20–50 mm every 1 mm), and give
  each ball a marking and location (e.g. *K-12 · case 2*). The calculator recommends the smallest
  ball of the active set that fits, together with its marking. The Balls tab marks the balls
  that fit the tube currently in the calculator.
- **Sharing ball sets.** A set can be shared as a QR code that you scan in the app on another
  phone, or as text that you send by message or e-mail and paste on import. Scanning runs
  fully on the device (CameraX + ZXing, no Google services). The camera permission is requested
  only when the scanner opens.
- **Recent tubes.** The last 8 tubes are kept and can be recalculated with one tap. A tube is
  saved only after its input has stayed unchanged for 2 s.
- **Share.** Sends a plain-text summary for protocols and messages.
- **Info.** Standard, method, disclaimer, privacy and app version.
- **Languages.** English, Polish and German. The app name is localized as well: *Ball Passage
  Test*, *Przelot kulki* (PL) and *Kugeldurchlauf* (DE). Library resources are limited to these
  three languages, so the UI is never mixed. Under the launcher icon the English name is
  shortened to *Ball Passage*, so that it is not cut off.

## Calculation

| Input | Symbol |
|---|---|
| Outer diameter | dₒ |
| Wall thickness | s |
| Smallest bend radius of the tube (0 = straight tube) | R |

1. Inner diameter: `dᵢ = dₒ − 2s`
2. Row: `dᵢ` rounded to whole millimetres (half up), 20–57 mm.
3. Column: bend ratio `R / dₒ`, ranges `[0.5, 1.0)`, `[1.0, 1.5)`, `[1.5, 2.0)`, `[2.0, ∞)`.
   A straight tube (R = 0) uses the last column.
4. Ball diameter per standard: `dᵢ − deduction`. This single value is the result. A ball from the
   set also fits if `dᵢ − deduction ≤ d < dᵢ`; such balls are listed as alternatives.

| dᵢ [mm] \ R/dₒ | 0.5–1.0 | 1.0–1.5 | 1.5–2.0 | ≥ 2.0 |
|---|---|---|---|---|
| 20–29 | 6 | 5.5 | 5 | 4 |
| 30–39 | 7 | 6.5 | 6 | 5 |
| 40–49 | 8 | 7.5 | 7 | 6 |
| 50–57 * | 9 | 8.5 | 8 | 7 |

\* Not part of the standard. Extrapolated values; the app always marks them as outside the standard.

All arithmetic uses `BigDecimal`, and the column bounds are compared as `R ≥ bound · dₒ`.
This means values exactly on a bound (e.g. R/dₒ = 1.0) are never misclassified by
floating-point rounding.

This app replaces the earlier *Kulowanie* web app (React PWA) and keeps its calculation
rules. Changes from the old app: the analytics are removed, an inverted validation
message is fixed, and the extrapolated row is marked.

## Project structure

| Module | Contents |
|---|---|
| `:core` | Pure Kotlin calculation (`BallPassageCalculator`, `BallPassageTable`), ball sets (`BallSets`, `BallRange`), sharing format (`BallSetCodec`) and ball selection (`BallSelector`), no Android dependencies |
| `:app` | Jetpack Compose UI (Material 3) with three tabs, ViewModels, DataStore persistence (`UserDataRepository`), resources in EN / PL / DE |

Tech: Kotlin, Jetpack Compose, Material 3 (dynamic color, dark theme), DataStore, AGP 9,
minSdk 26, targetSdk 36. No network permission, no analytics, no third-party SDKs. Android
backup includes only the DataStore file, which holds the ball set and recent tubes.

## Building

Requirements: JDK 17+ and the Android SDK. Missing SDK platforms are downloaded by Gradle on the first build.

```sh
./gradlew check            # unit tests (core + app, incl. Robolectric UI tests) and lint
./gradlew connectedDebugAndroidTest  # instrumented tests (dialog flows) on a device or emulator
./gradlew assembleDebug    # debug APK
./gradlew bundleRelease    # release App Bundle (R8 minified)
```

Lint warnings fail the build, and so do Kotlin warnings in the app module.

Instrumented tests (`app/src/androidTest`) cover the dialogs of the Balls tab. They run on a
device because Robolectric never gets idle after typing into text fields inside Compose
dialogs. Real devices render those dialogs normally, which was checked by counting frames.

## Release signing

Release builds are signed with the **upload key** when it is configured. Otherwise they are
built unsigned. Google Play App Signing holds the app signing key.

Local builds read `keystore.properties` in the project root. This file is git-ignored:

```properties
storeFile=/absolute/path/to/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

The Release workflow signs with the same key, kept as secrets of the `release` environment:
`UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS` and
`UPLOAD_KEY_PASSWORD` (see [Publishing to Google Play](#publishing-to-google-play)).

To create the upload key:

```sh
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 upload.jks   # value for UPLOAD_KEYSTORE_BASE64
```

## Store listing

The `fastlane/metadata/android/` folder holds the Google Play listing in EN, PL and DE:
titles, short and full descriptions, changelogs, the 512 px icon, the feature graphic and
phone screenshots. The folder layout follows the
[fastlane supply](https://docs.fastlane.tools/actions/supply/) format, so you can upload it
by hand or with `fastlane supply`. The Promote workflow uploads it to Google Play. The privacy
policy is in [PRIVACY.md](PRIVACY.md).

## Releasing a new version

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
3. Merge to `main`, then tag and push, for example `git tag -a v1.0.3 -m "Version 1.0.3"` and
   `git push origin v1.0.3`.
4. Approve the **Release** run in GitHub Actions. It runs the tests, builds the signed bundle and
   uploads it with the changelogs to the **internal testing** track.
5. Check the internal build on a phone. Then run **Actions → Promote to production → Run
   workflow**. After approval it moves the release to production and uploads the store listing
   (texts and images). Google reviews the changes before they appear.

## Publishing to Google Play

Publishing uses [fastlane](https://docs.fastlane.tools/actions/upload_to_play_store/)
(`fastlane/Fastfile`). GitHub Actions signs in to Google with Workload Identity Federation, so
the repository holds no Google credentials. Only this repository, in jobs of the `release`
environment, can act as the publishing service account.

The `release` environment:

- accepts only `v*` tags and `main`, and waits for approval by the owner,
- holds the upload key secrets (see [Release signing](#release-signing)),
- holds two variables: `GCP_WORKLOAD_IDENTITY_PROVIDER` (the provider's full resource name) and
  `GCP_SERVICE_ACCOUNT` (the service account e-mail).

Google Cloud setup, done once in Cloud Shell:

```sh
PROJECT_ID=ball-passage-publishing          # any free project ID
REPO_ID=1431022668                          # gh api repos/lukaszstalmasinski/ball-passage-test -q .id
gcloud projects create "$PROJECT_ID"
gcloud config set project "$PROJECT_ID"
gcloud services enable androidpublisher.googleapis.com iam.googleapis.com \
  iamcredentials.googleapis.com sts.googleapis.com
gcloud iam service-accounts create play-publisher --display-name="Google Play publisher"
gcloud iam workload-identity-pools create github --location=global --display-name="GitHub Actions"
gcloud iam workload-identity-pools providers create-oidc ball-passage-test \
  --location=global --workload-identity-pool=github --display-name="ball-passage-test" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository_id=assertion.repository_id,attribute.environment=assertion.environment" \
  --attribute-condition="assertion.repository_id == '$REPO_ID' && assertion.environment == 'release'"
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
gcloud iam service-accounts add-iam-policy-binding \
  "play-publisher@$PROJECT_ID.iam.gserviceaccount.com" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository_id/$REPO_ID"
```

Then in Play Console, **Users and permissions → Invite new users**: invite the service account
e-mail and give it, for this app, the permissions to release to testing tracks and production
and to manage the store listing.

## License

Copyright © 2026 Łukasz Stalmasiński. **All rights reserved.**

The source code is public for reference only. It may not be copied, modified, distributed or
used in other projects without written permission. See [LICENSE](LICENSE).
