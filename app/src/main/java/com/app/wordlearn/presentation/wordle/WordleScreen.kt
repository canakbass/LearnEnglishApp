package com.app.wordlearn.presentation.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.wordlearn.domain.model.LetterResult
import com.app.wordlearn.presentation.theme.*

@Composable
fun WordleScreen(viewModel: WordleViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startNewGame()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Wordle",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Tahmin grid'i (6 satır x 5 sütun)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 6) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 5) {
                            val letter: String
                            val bgColor: Color

                            if (row < gameState.attempts.size) {
                                // Tamamlanmış tahmin
                                letter = gameState.attempts[row][col].toString()
                                bgColor = when (gameState.guessResults[row][col]) {
                                    LetterResult.CORRECT -> WordleCorrect
                                    LetterResult.PRESENT -> WordlePresent
                                    LetterResult.ABSENT -> WordleAbsent
                                    LetterResult.UNUSED -> WordleEmpty
                                }
                            } else if (row == gameState.attempts.size) {
                                // Aktif tahmin satırı
                                letter = gameState.currentGuess.getOrNull(col)?.toString() ?: ""
                                bgColor = Color.Transparent
                            } else {
                                letter = ""
                                bgColor = Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(
                                        2.dp,
                                        if (bgColor == Color.Transparent)
                                            WordleEmpty
                                        else
                                            bgColor,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(bgColor, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bgColor != Color.Transparent) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oyun sonu mesajı
            if (gameState.isGameOver) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (gameState.isWon)
                            Success.copy(alpha = 0.1f)
                        else
                            Error.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (gameState.isWon) "Tebrikler! 🎉"
                            else "Kaybettiniz!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!gameState.isWon) {
                            Text(
                                "Doğru kelime: ${gameState.targetWord}",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.startNewGame() }) {
                            Text("Yeni Oyun")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Klavye
            val keyboardRows = listOf(
                "QWERTYUIOP",
                "ASDFGHJKL",
                "ZXCVBNM"
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keyboardRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (row == "ZXCVBNM") {
                            // Enter butonu
                            Button(
                                onClick = { viewModel.onSubmit() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1.5f),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("GO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        row.forEach { char ->
                            val keyState = gameState.keyboardState[char]
                            val keyColor = when (keyState) {
                                LetterResult.CORRECT -> WordleCorrect
                                LetterResult.PRESENT -> WordlePresent
                                LetterResult.ABSENT -> WordleAbsent
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            Button(
                                onClick = { viewModel.onKeyPress(char) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = keyColor
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    char.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (keyState != null) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (row == "ZXCVBNM") {
                            // Backspace butonu
                            Button(
                                onClick = { viewModel.onBackspace() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .weight(1.5f),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("⌫", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
