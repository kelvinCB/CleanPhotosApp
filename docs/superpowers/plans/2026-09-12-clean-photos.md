# Clean Photos Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Crear y validar una app Android ligera para revisar todas las fotos del dispositivo en lotes de 200 y mover las seleccionadas a la papelera real.

**Architecture:** Un repositorio Android consulta únicamente metadatos de `MediaStore.Images`. Una máquina de estados pura administra el lote y el resumen; Compose presenta una tarjeta deslizante y `MediaStore.createTrashRequest` realiza la operación confirmada por Android.

**Tech Stack:** Kotlin, Android SDK, Jetpack Compose, Material 3, Coil Compose, JUnit, Gradle Android plugin.

**Spec:** `docs/superpowers/specs/2026-09-12-clean-photos-design.md`

## Global Constraints

- Android 11+ (`minSdk 30`) para garantizar la API oficial de papelera.
- Incluir todas las imágenes indexadas por `MediaStore.Images`, excluyendo vídeos y elementos ya enviados a papelera.
- Solicitar `READ_MEDIA_IMAGES` en Android 13+ y `READ_EXTERNAL_STORAGE` en Android 12 o anterior.
- No copiar fotos ni usar red; cargar miniaturas bajo demanda.
- Solo contar una eliminación después de `RESULT_OK` de la confirmación del sistema.
- No modificar `sources/` ni archivos de otros proyectos.

### Task 1: Proyecto Android mínimo y máquina de estados

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/domain/Photo.kt`
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/domain/ReviewSession.kt`
- Test: `app/src/test/java/com/kelvincalcano/cleanphotos/domain/ReviewSessionTest.kt`

**Interfaces:**
- `data class Photo(val id: Long, val uri: Uri, val displayName: String, val sizeBytes: Long)`.
- `class ReviewSession(private val batchSize: Int = 200)` with `current: Photo?`, `reviewedCount`, `keptCount`, `trashedCount`, `trashedBytes`, `isBatchComplete`, `hasMorePhotos`, `load(photos: List<Photo>)`, `keepCurrent()`, `requestTrashCurrent(): Photo?`, `confirmTrash(photoId: Long)`, `cancelTrash(photoId: Long)` and `finishBatch()`.
- `fun formatBytes(bytes: Long): String` for user-facing summaries.

- [ ] **Step 1: Write the failing test**

Add tests proving that a loaded batch exposes its first photo, left decisions advance and count as kept, confirmed trash advances and sums bytes, cancelled trash does not advance, and a batch caps at 200 items.

- [ ] **Step 2: Run test to verify it fails**

Run: `.gradlew.bat :app:testDebugUnitTest --tests '*ReviewSessionTest' --no-daemon --offline --console=plain`

Expected: FAIL because the domain classes do not yet exist.

- [ ] **Step 3: Write minimal implementation**

Implement the immutable `Photo`, the explicit pending-trash state in `ReviewSession`, and byte formatting. Keep all Android-independent behavior in the domain package.

- [ ] **Step 4: Run test to verify it passes**

Run the same focused test command. Expected: all `ReviewSessionTest` tests PASS.

- [ ] **Step 5: Commit**

Do not commit automatically; retain the working tree for the user to review.

### Task 2: MediaStore repository and permissions

**Files:**
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/data/PhotoRepository.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/kelvincalcano/cleanphotos/data/PhotoRepositoryContractTest.kt`

**Interfaces:**
- `class PhotoRepository(private val resolver: ContentResolver)`.
- `suspend fun loadPhotos(): List<Photo>` queries `MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)` for `_ID`, `DISPLAY_NAME`, `SIZE`, and `DATE_ADDED`, with `IS_TRASHED = 0`, ascending by `DATE_ADDED` then `_ID`.
- `fun createTrashRequest(photo: Photo): IntentSender` delegates to `MediaStore.createTrashRequest(resolver, listOf(photo.uri), true).intentSender`.

- [ ] **Step 1: Write the failing contract test**

Define the expected projection, selection, sort order, and single-photo trash request behavior against a small fake resolver boundary.

- [ ] **Step 2: Run the focused test and confirm the expected failure**

Run: `.gradlew.bat :app:testDebugUnitTest --tests '*PhotoRepositoryContractTest' --no-daemon --offline --console=plain`

Expected: FAIL because `PhotoRepository` is not implemented.

- [ ] **Step 3: Implement the repository**

Use `ContentResolver.query` with a `use` block, map rows defensively, ignore rows with missing IDs/URIs, and close the cursor. Add manifest permissions and platform-aware permission constants in the activity layer.

- [ ] **Step 4: Run the focused test and then all unit tests**

Run the focused command, then `.gradlew.bat :app:testDebugUnitTest --no-daemon --offline --console=plain`. Expected: PASS with no failures.

- [ ] **Step 5: Commit**

Do not commit automatically; retain the working tree for the user to review.

### Task 3: Compose review flow and system-trash result handling

**Files:**
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/MainActivity.kt`
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/ui/ReviewScreen.kt`
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/ui/PhotoCard.kt`
- Create: `app/src/main/java/com/kelvincalcano/cleanphotos/ui/PermissionScreen.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

**Interfaces:**
- `ReviewScreen(session: ReviewSession, onKeep: () -> Unit, onTrash: () -> Unit, onLoadMore: () -> Unit)`.
- `PhotoCard(photo: Photo, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit)`.
- `MainActivity` owns the `ActivityResultLauncher<IntentSenderRequest>`, keeps the pending `Photo`, and calls `confirmTrash` only on `RESULT_OK`.

- [ ] **Step 1: Add UI test scaffolding and verify the missing screen fails to compile**

Create a Compose test that asserts the permission CTA, current photo content description, left/right action labels, and “Cargar 200 más” summary action.

- [ ] **Step 2: Implement the minimal Compose flow**

Request full photo access, show a clear partial-access message, use `AsyncImage` with content URIs, detect horizontal drag direction, animate card exit, expose accessible buttons as a fallback, and render a summary after each batch.

- [ ] **Step 3: Wire trash confirmation**

Launch `IntentSenderRequest` for the current photo, keep the card pending while Android asks for confirmation, then show “Enviada a papelera” and update saved bytes only for a successful result. On cancellation, restore the card and show an error message.

- [ ] **Step 4: Run unit and Compose tests**

Run: `.gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest --no-daemon --offline --console=plain`. Expected: unit and instrumentation tests PASS when a test target is available.

- [ ] **Step 5: Commit**

Do not commit automatically; retain the working tree for the user to review.

### Task 4: Build, install, and physical-device verification

**Files:**
- Create: `README.md`
- Create: `verification/physical-device-checklist.md`

- [ ] **Step 1: Build the debug APK**

Run: `.gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --offline --console=plain` with the Android SDK and Java 17 configured for this session.

- [ ] **Step 2: Install only the new package on the authorized device**

Resolve the APK path, record its SHA-256 and package/version metadata, then run `adb -s R5CY5249KYX install -r <apk>`.

- [ ] **Step 3: Capture initial device evidence**

Use `adb shell cmd package resolve-activity`, UI-tree dump, screenshot, and logcat crash buffer to confirm launch, permission screen, and no startup crash.

- [ ] **Step 4: Exercise a safe physical flow**

Grant photo access through the visible Android permission UI, verify that a real image appears, perform one keep swipe and one trash swipe, accept the Android confirmation, and verify the selected URI is trashed through `MediaStore`/device gallery evidence. Do not delete additional personal photos without an explicit per-action confirmation from the user during the live test.

- [ ] **Step 5: Record limitations and final status**

Report build/install, permission, swipe/animation, trash confirmation, byte summary, and crash evidence separately. Distinguish “moved to paper” from immediate physical free-space increase.

