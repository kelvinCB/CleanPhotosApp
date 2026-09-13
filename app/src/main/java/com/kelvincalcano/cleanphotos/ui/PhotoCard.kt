package com.kelvincalcano.cleanphotos.ui

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kelvincalcano.cleanphotos.domain.Photo
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoCard(
    photo: Photo,
    resolver: ContentResolver,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    isPending: Boolean,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember(photo.id) { Animatable(0f) }
    var exiting by remember(photo.id) { androidx.compose.runtime.mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isPending) {
        if (!isPending) {
            exiting = false
            offsetX.snapTo(0f)
        }
    }

    fun finishSwipe(right: Boolean) {
        if (exiting || isPending) return
        exiting = true
        val target = if (right) 1_200f else -1_200f
        scope.launch {
            offsetX.animateTo(target, tween(220))
            if (right) onSwipeRight() else onSwipeLeft()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
            .pointerInput(photo.id, isPending) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (!isPending && !exiting) {
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                            }
                        }
                    },
                    onDragEnd = {
                        when {
                            offsetX.value > SWIPE_THRESHOLD -> finishSwipe(right = true)
                            offsetX.value < -SWIPE_THRESHOLD -> finishSwipe(right = false)
                            else -> scope.launch {
                                offsetX.animateTo(0f, tween(180))
                            }
                        }
                    },
                )
            }
            .graphicsLayer { translationX = offsetX.value }
            .semantics { contentDescription = "Foto ${photo.displayName}" },
    ) {
        val label = when {
            offsetX.value > 48f -> "PAPELERA"
            offsetX.value < -48f -> "CONSERVAR"
            else -> null
        }
        val labelColor = if (offsetX.value > 48f) Color(0xFFE53935) else Color(0xFF2E7D32)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            PhotoThumbnail(photo, resolver)
            if (label != null) {
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: Photo, resolver: ContentResolver) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = ImageDecoder.createSource(resolver, Uri.parse(photo.uri))
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(900, 1_200)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .size(420.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(420.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Cargando foto…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val SWIPE_THRESHOLD = 180f
