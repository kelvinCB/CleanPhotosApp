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
import com.kelvincalcano.cleanphotos.data.PhotoRepository
import com.kelvincalcano.cleanphotos.domain.BatchWriteAccessRegistry
import com.kelvincalcano.cleanphotos.domain.KeptPhotoStore
import com.kelvincalcano.cleanphotos.domain.ReviewStateHolder
import com.kelvincalcano.cleanphotos.ui.LoadingState
import com.kelvincalcano.cleanphotos.ui.PermissionScreen
import com.kelvincalcano.cleanphotos.ui.ReviewScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: PhotoRepository
    private lateinit var keptPhotoStore: KeptPhotoStore
    private val reviewState = ReviewStateHolder()
    private val batchWriteAccess = BatchWriteAccessRegistry()
    private var reviewSnapshot by mutableStateOf(reviewState.state)
    private var hasFullAccess by mutableStateOf(false)
    private var hasPartialAccess by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)
    private var feedback by mutableStateOf<String?>(null)
    private var pendingTrashId: Long? = null
    private var pendingWriteUris: List<Uri> = emptyList()
    private var requestedWriteBatchKey: String? = null
    private var isRequestingBatchWriteAccess by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshAccessState()
        if (hasFullAccess) loadPhotos()
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
        if (hasFullAccess) loadPhotos()
        setContent {
            MaterialTheme {
                when {
                    !hasFullAccess -> PermissionScreen(
                        partialAccess = hasPartialAccess,
                        onRequestPermission = ::requestPhotoPermission,
                        onOpenSettings = ::openAppSettings,
                    )
                    isLoading -> LoadingState()
                    else -> ReviewScreen(
                        state = reviewSnapshot,
                        onKeep = ::keepCurrent,
                        onTrash = ::trashCurrent,
                        onLoadMore = ::loadMore,
                        feedback = feedback,
                        actionsEnabled = !isRequestingBatchWriteAccess,
                    )
                }

                val batchKey = reviewState.currentBatch.firstOrNull()?.uri
                LaunchedEffect(batchKey, isLoading, hasFullAccess) {
                    if (!isLoading && hasFullAccess && batchKey != null && batchKey != requestedWriteBatchKey) {
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
            if (!wasFull && hasFullAccess) loadPhotos()
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

    private fun loadPhotos() {
        isLoading = true
        lifecycleScope.launch {
            reviewState.load(keptPhotoStore.filterUnreviewed(repository.loadPhotos()))
            reviewSnapshot = reviewState.state
            isLoading = false
            feedback = null
        }
    }

    private fun keepCurrent() {
        val photo = reviewState.state.current ?: return
        if (reviewState.keepCurrent()) {
            keptPhotoStore.markKept(photo)
            reviewSnapshot = reviewState.state
            feedback = "Conservada"
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
        requestedWriteBatchKey = batch.first().uri
        if (missingUris.isEmpty()) return
        pendingWriteUris = missingUris.map(Uri::parse)
        isRequestingBatchWriteAccess = true
        writeAccessLauncher.launch(
            IntentSenderRequest.Builder(repository.createWriteRequest(batch.filter { it.uri in missingUris })).build(),
        )
    }

    private companion object {
        const val KEPT_PHOTOS_PREFERENCES = "kept_photos"
        const val KEPT_PHOTOS_KEY = "kept_uris"
    }
}
