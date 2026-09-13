package com.kelvincalcano.cleanphotos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.kelvincalcano.cleanphotos.domain.PhotoAlbum
import com.kelvincalcano.cleanphotos.domain.BatchWritePromptController
import com.kelvincalcano.cleanphotos.data.PhotoRepository
import com.kelvincalcano.cleanphotos.domain.BatchWriteAccessRegistry
import com.kelvincalcano.cleanphotos.domain.KeptPhotoStore
import com.kelvincalcano.cleanphotos.domain.ReviewStateHolder
import com.kelvincalcano.cleanphotos.ui.LoadingState
import com.kelvincalcano.cleanphotos.ui.AlbumSelectionScreen
import com.kelvincalcano.cleanphotos.ui.PermissionScreen
import com.kelvincalcano.cleanphotos.ui.ReviewScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: PhotoRepository
    private lateinit var keptPhotoStore: KeptPhotoStore
    private val reviewState = ReviewStateHolder()
    private val batchWriteAccess = BatchWriteAccessRegistry()
    private val batchWritePrompt = BatchWritePromptController()
    private var reviewSnapshot by mutableStateOf(reviewState.state)
    private var albums by mutableStateOf<List<PhotoAlbum>>(emptyList())
    private var selectedAlbum by mutableStateOf<PhotoAlbum?>(null)
    private var hasFullAccess by mutableStateOf(false)
    private var hasPartialAccess by mutableStateOf(false)
    private var isLoadingAlbums by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)
    private var feedback by mutableStateOf<String?>(null)
    private var pendingTrashId: Long? = null
    private var pendingWriteUris: List<Uri> = emptyList()
    private var showBatchWriteExplanation by mutableStateOf(false)
    private var isRequestingBatchWriteAccess by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshAccessState()
        if (hasFullAccess) loadAlbums()
    }

    private val trashLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val photoId = pendingTrashId ?: return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            reviewState.confirmTrash(photoId)
            reviewSnapshot = reviewState.state
            feedback = "Enviada a papelera"
        } else {
            reviewState.cancelTrash(photoId)
            reviewSnapshot = reviewState.state
            feedback = "No se movió la foto a la papelera"
        }
        pendingTrashId = null
    }

    private val writeAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val requestedUris = pendingWriteUris
        pendingWriteUris = emptyList()
        batchWritePrompt.finishNativeRequest()
        isRequestingBatchWriteAccess = false
        if (result.resultCode == Activity.RESULT_OK) {
            batchWriteAccess.grant(requestedUris.map(Uri::toString))
            feedback = "Papelera autorizada para este lote"
        } else {
            feedback = "Android pedirá confirmación individual si envías una foto a papelera"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        repository = PhotoRepository(contentResolver)
        val keptPreferences = getSharedPreferences(KEPT_PHOTOS_PREFERENCES, MODE_PRIVATE)
        keptPhotoStore = KeptPhotoStore(
            initiallyKeptUris = keptPreferences.getStringSet(KEPT_PHOTOS_KEY, emptySet()).orEmpty(),
        ) { keptUris ->
            keptPreferences.edit().putStringSet(KEPT_PHOTOS_KEY, keptUris).apply()
        }
        refreshAccessState()
        if (hasFullAccess) loadAlbums()
        setContent {
            MaterialTheme {
                when {
                    !hasFullAccess -> PermissionScreen(
                        partialAccess = hasPartialAccess,
                        onRequestPermission = ::requestPhotoPermission,
                        onOpenSettings = ::openAppSettings,
                    )
                    isLoadingAlbums -> LoadingState("Buscando álbumes…")
                    selectedAlbum == null -> AlbumSelectionScreen(
                        albums = albums,
                        onAlbumSelected = ::selectAlbum,
                    )
                    isLoading -> LoadingState()
                    else -> ReviewScreen(
                        state = reviewSnapshot,
                        onKeep = ::keepCurrent,
                        onTrash = ::trashCurrent,
                        onLoadMore = ::loadMore,
                        onUndo = ::undoLastDecision,
                        feedback = feedback,
                        actionsEnabled = !isRequestingBatchWriteAccess,
                        showBatchWriteExplanation = showBatchWriteExplanation,
                        onBatchWriteExplanationAccepted = ::acknowledgeBatchWriteExplanation,
                        onBatchWriteExplanationDismissed = ::dismissBatchWriteExplanation,
                        albumName = selectedAlbum?.name,
                        albumPhotoCount = selectedAlbum?.photoCount,
                        onChangeAlbum = ::showAlbumSelection,
                        onReactivatePhotos = ::reactivatePhotos,
                    )
                }

                val batchKey = reviewState.currentBatch.firstOrNull()?.uri
                LaunchedEffect(batchKey, isLoading, hasFullAccess, selectedAlbum?.id) {
                    if (!isLoading && hasFullAccess && selectedAlbum != null && batchKey != null) {
                        requestBatchWriteAccess()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            val wasFull = hasFullAccess
            refreshAccessState()
            if (!wasFull && hasFullAccess) loadAlbums()
        }
    }

    private fun requestPhotoPermission() {
        permissionLauncher.launch(requiredReadPermission())
    }

    private fun refreshAccessState() {
        hasFullAccess = checkSelfPermission(requiredReadPermission()) == PackageManager.PERMISSION_GRANTED
        hasPartialAccess = Build.VERSION.SDK_INT >= 34 &&
            checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED &&
            !hasFullAccess
    }

    private fun requiredReadPermission(): String = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun loadAlbums() {
        isLoadingAlbums = true
        lifecycleScope.launch {
            albums = repository.loadAlbums()
            selectedAlbum = null
            isLoadingAlbums = false
        }
    }

    private fun selectAlbum(album: PhotoAlbum) {
        selectedAlbum = album
        loadPhotos(album)
    }

    private fun loadPhotos(album: PhotoAlbum) {
        isLoading = true
        lifecycleScope.launch {
            reviewState.load(keptPhotoStore.filterUnreviewed(repository.loadPhotos(album)))
            reviewSnapshot = reviewState.state
            isLoading = false
            feedback = null
        }
    }

    private fun showAlbumSelection() {
        selectedAlbum = null
        feedback = null
    }

    private fun reactivatePhotos() {
        val album = selectedAlbum ?: return
        isLoading = true
        lifecycleScope.launch {
            val photos = repository.loadPhotos(album)
            keptPhotoStore.unmarkKept(photos)
            reviewState.load(photos)
            reviewSnapshot = reviewState.state
            isLoading = false
            feedback = "Fotos reactivadas para clasificar"
        }
    }

    private fun keepCurrent() {
        val photo = reviewState.state.current ?: return
        if (reviewState.keepCurrent()) {
            keptPhotoStore.markKept(photo)
            reviewSnapshot = reviewState.state
            feedback = null
        }
    }

    private fun trashCurrent() {
        val photo = reviewState.requestTrashCurrent() ?: return
        if (batchWriteAccess.hasAccess(photo)) {
            val moved = runCatching { repository.moveToTrash(photo) }.getOrDefault(false)
            if (moved) {
                reviewState.confirmTrash(photo.id)
                reviewSnapshot = reviewState.state
                feedback = "Enviada a papelera"
                return
            }
            reviewState.cancelTrash(photo.id)
            reviewSnapshot = reviewState.state
        }
        val pendingPhoto = if (reviewState.state.pendingTrash) {
            photo
        } else {
            reviewState.requestTrashCurrent() ?: return
        }
        reviewSnapshot = reviewState.state
        pendingTrashId = pendingPhoto.id
        feedback = "Confirma en Android para moverla a papelera"
        trashLauncher.launch(IntentSenderRequest.Builder(repository.createTrashRequest(pendingPhoto)).build())
    }

    private fun loadMore() {
        if (reviewState.loadNextBatch()) {
            reviewSnapshot = reviewState.state
            feedback = null
        }
    }

    private fun undoLastDecision() {
        val undone = reviewState.undoLastDecision() ?: return
        keptPhotoStore.unmarkKept(undone.photo)
        reviewSnapshot = reviewState.state
        feedback = "Foto recuperada"
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun requestBatchWriteAccess() {
        val batch = reviewState.currentBatch
        if (batch.isEmpty()) return
        val missingUris = batchWriteAccess.missingUris(batch)
        if (!batchWritePrompt.prepare(batch.first().uri, missingUris)) return
        pendingWriteUris = missingUris.map(Uri::parse)
        showBatchWriteExplanation = batchWritePrompt.isExplanationVisible
    }

    private fun acknowledgeBatchWriteExplanation() {
        val requestedUris = batchWritePrompt.acknowledgeExplanation()
        pendingWriteUris = requestedUris.map(Uri::parse)
        showBatchWriteExplanation = false
        isRequestingBatchWriteAccess = true
        writeAccessLauncher.launch(
            IntentSenderRequest.Builder(repository.createWriteRequest(reviewState.currentBatch.filter { it.uri in requestedUris })).build(),
        )
    }

    private fun dismissBatchWriteExplanation() {
        batchWritePrompt.dismissExplanation()
        showBatchWriteExplanation = false
        isRequestingBatchWriteAccess = false
        feedback = "Puedes autorizar el permiso desde el botón Papelera si lo necesitas"
    }

    private companion object {
        const val KEPT_PHOTOS_PREFERENCES = "kept_photos"
        const val KEPT_PHOTOS_KEY = "kept_uris"
    }
}
