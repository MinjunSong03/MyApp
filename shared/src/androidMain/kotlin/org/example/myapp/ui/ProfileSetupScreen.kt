package org.example.myapp.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.model.AuthState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import org.example.myapp.auth.model.PickedMedia
import org.example.myapp.auth.network.MediaType
import org.example.myapp.auth.viewmodel.ProfileSetupViewModel
import org.example.myapp.util.toPickedMedia
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var selectedImage by remember { mutableStateOf<PickedMedia?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()

    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val media = uri.toPickedMedia(context)
            if (media != null && media.mediaType == MediaType.IMAGE) {
                selectedImage = media
            } else if (media != null) {
                Toast.makeText(context, "사진 파일만 선택할 수 있습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "파일을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val state = authState
    val initialNickname = if (state is AuthState.Authenticated) {
        state.session.nickname ?: ""
    } else ""

    var inputNickname by rememberSaveable(initialNickname) { mutableStateOf(initialNickname) }

    val previewBitmap = remember(selectedImage) {
        selectedImage?.let {
            runCatching {
                BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "환영합니다!",
            fontSize = 28.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "프로필 사진과 닉네임을 설정해 주세요.",
            fontSize = 14.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable {
                    singleImagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = "선택된 프로필 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = null,
                    contentDescription = "기본 프로필 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "사진 추가",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }

        if (selectedImage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedImage?.fileName ?: "선택된 사진",
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { selectedImage = null }) {
                        Text(text = "삭제", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = inputNickname,
            onValueChange = { inputNickname = it },
            label = { Text(text = "닉네임") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                viewModel.updateProfile(
                    nickname = inputNickname.trim(),
                    selectedImage = selectedImage
                )
            },
            enabled = inputNickname.trim().isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "시작하기",
                    color = Color.White
                )
            }
        }
    }
}