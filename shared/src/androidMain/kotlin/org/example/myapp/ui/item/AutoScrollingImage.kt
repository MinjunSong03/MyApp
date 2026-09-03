package org.example.myapp.ui.item

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AutoScrollingImage(
    images: List<Int>,
    modifier: Modifier = Modifier,
    speed: Float = 2.0f
) {
    if (images.isEmpty()) return

    val infiniteItemCount = Int.MAX_VALUE
    val startIndex = remember(images.size) {
        (infiniteItemCount / 2) - ((infiniteItemCount / 2) % images.size)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            while (isActive) {
                listState.scroll(MutatePriority.Default) {
                    scrollBy(speed)
                }
                delay(16L)
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = infiniteItemCount,
            key = { index -> index }
        ) { index ->
            val realIndex = index % images.size
            val imageUrl = images[realIndex]

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 180.dp, height = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}
