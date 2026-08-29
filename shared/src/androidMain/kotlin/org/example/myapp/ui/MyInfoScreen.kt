package org.example.myapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import org.example.myapp.auth.model.AuthState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import org.example.myapp.auth.model.OAuthProvider
import org.example.myapp.auth.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyInfoScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onUpdateNicknameClick: () -> Unit,
    onMyPostClick: () -> Unit,
    onManageMyClick: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    val session = when (val state = authState) {
        is AuthState.Authenticated -> state.session
        else -> null
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${session?.nickname} 님.",
                color = Color.Black,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "반갑습니다!",
                color = Color.Black,
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "고유번호: ${session?.userId ?: "-"}",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onUpdateNicknameClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "닉네임 변경",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onMyPostClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "나의 게시물",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onManageMyClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(
                    text = "차단 관리",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.logout(OAuthProvider.KAKAO) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text(
                    text = "로그아웃",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.unlink(OAuthProvider.KAKAO) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
            ) {
                Text(
                    text = "회원탈퇴",
                    color = Color.White
                )
            }
        }
    }
}