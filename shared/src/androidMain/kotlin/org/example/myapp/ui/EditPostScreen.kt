package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.viewmodel.PostViewModel
import org.example.myapp.shared.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditPostScreen(
    postId: Long,
    viewModel: PostViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var mediaType by rememberSaveable { mutableStateOf(MediaType.IMAGE) }
    var mediaUrl by rememberSaveable { mutableStateOf("") }
    var thumbnailUrl by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isInitialDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(postId) {
        if (!isInitialDataLoaded) {
            val existingPost = viewModel.getPostById(postId)
            if (existingPost != null) {
                title = existingPost.title
                description = existingPost.description
                mediaType = existingPost.mediaType
                mediaUrl = existingPost.mediaUrl
                thumbnailUrl = existingPost.thumbnailUrl
                isInitialDataLoaded = true
            } else {
                Toast.makeText(context, "게시물 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                onBack()
            }
        }
    }

    if (!isInitialDataLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Black)
        }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateSuccessEvent.collect {
            onBack()
        }
    }

    val isFormValid = title.isNotBlank() && description.isNotBlank() && mediaUrl.isNotBlank() && thumbnailUrl.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "뒤로가기",
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "게시물 수정",
                fontSize = 18.sp,
                color = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "미디어 형식",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mediaType == MediaType.IMAGE,
                    onClick = { mediaType = MediaType.IMAGE }
                )
                Text(text = "이미지")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = mediaType == MediaType.VIDEO,
                    onClick = { mediaType = MediaType.VIDEO }
                )
                Text(text = "동영상")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = "제목") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "내용") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("미디어(S3) URL") },
                placeholder = { Text("https://...") },
                minLines = 1,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = thumbnailUrl,
                onValueChange = { thumbnailUrl = it },
                label = { Text("썸네일 URL") },
                minLines = 1,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    isLoading = true
                    viewModel.editPost(
                        postId = postId,
                        title = title.trim(),
                        description = description.trim(),
                        mediaType = mediaType,
                        thumbnailUrl = thumbnailUrl.trim(),
                        mediaUrl = mediaUrl.trim()
                    )
                },
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "게시물 수정",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}