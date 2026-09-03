package org.example.myapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.myapp.auth.model.AuthState
import org.example.myapp.auth.viewmodel.DetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetailScreen(
    viewModel: DetailViewModel = koinViewModel(),
    onBack: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.updateSuccessEvent.collect {
            onBack()
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

    var inputNickname by rememberSaveable { mutableStateOf(initialNickname) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "닉네임 변경",
                fontSize = 28.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "사용할 닉네임을 입력해 주세요.",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputNickname,
                onValueChange = { inputNickname = it },
                label = { Text( text = "닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    cursorColor = MaterialTheme.colorScheme.outline,
                    selectionColors = TextSelectionColors(
                        handleColor = Color.Black,
                        backgroundColor = Color.Black.copy(alpha = 0.2f)
                    )
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.updateNickname(inputNickname.trim()) },
                enabled = inputNickname.isNotBlank() && authState !is AuthState.Loading && inputNickname != initialNickname,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "변경",
                    color = Color.White
                )
            }
        }
    }
}