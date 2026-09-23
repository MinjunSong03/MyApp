package org.example.myapp.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.myapp.auth.network.UserResponse
import org.example.myapp.shared.R

@Composable
fun UserCard(
    user: UserResponse,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = true,
    onLikeClick: ((Long) -> Unit)? = null,
    onUnblockClick: ((Long) -> Unit)? = null,
    onBlockClick: ((Long) -> Unit)? = null,
    onReportClick: ((Long) -> Unit)? = null
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val hasOptionsMenu = onUnblockClick != null || onBlockClick != null || onReportClick != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable { if (!user.isDeleted) onProfileClick(user.id) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = "프로필 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = user.nickname,
                        fontWeight = FontWeight.Bold,
                        color = if (user.isDeleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onLikeClick != null) {
                    IconButton(
                        onClick = { onLikeClick(user.id) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like
                            ),
                            contentDescription = if (isLiked) "좋아요 취소" else "좋아요",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (hasOptionsMenu) {
                    Box {
                        IconButton(
                            onClick = { isMenuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_option),
                                contentDescription = "옵션",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            if (onUnblockClick != null) {
                                DropdownMenuItem(
                                    text = { Text(text = "차단 해제") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onUnblockClick(user.id)
                                    }
                                )
                            }
                            if (onBlockClick != null) {
                                DropdownMenuItem(
                                    text = { Text(text = "이 사용자 차단하기") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onBlockClick(user.id)
                                    }
                                )
                            }
                            if (onReportClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "이 사용자 신고하기",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        onReportClick(user.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}