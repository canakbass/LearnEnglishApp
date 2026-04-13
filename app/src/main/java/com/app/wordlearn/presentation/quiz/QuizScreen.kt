package com.app.wordlearn.presentation.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.wordlearn.presentation.theme.Error
import com.app.wordlearn.presentation.theme.Success

@Composable
fun QuizScreen(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDailyQuiz()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = uiState) {
            is QuizUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quiz hazırlanıyor...")
                }
            }

            is QuizUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Bugün için soru yok!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tüm kelimeleri tekrar ettiniz.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            is QuizUiState.QuizActive -> {
                // İlerleme çubuğu
                LinearProgressIndicator(
                    progress = (state.questionIndex + 1f) / state.totalQuestions,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Soru ${state.questionIndex + 1} / ${state.totalQuestions}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "Doğru: ${state.score}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Success
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Soru kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bu kelimenin Türkçe karşılığı nedir?", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            state.currentQuestion.questionText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4 şık
                state.currentQuestion.options.forEach { option ->
                    OutlinedButton(
                        onClick = { viewModel.submitAnswer(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(option, fontSize = 16.sp)
                    }
                }
            }

            is QuizUiState.Feedback -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isCorrect)
                                Success.copy(alpha = 0.1f)
                            else
                                Error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (state.isCorrect)
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (state.isCorrect) Success else Error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (state.isCorrect) "Doğru! ✓" else "Yanlış ✗",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isCorrect) Success else Error
                            )
                            if (!state.isCorrect) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Doğru cevap: ${state.correctAnswer}",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            is QuizUiState.Summary -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Quiz Tamamlandı!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val percentage = if (state.totalQuestions > 0)
                                (state.correctCount * 100) / state.totalQuestions else 0

                            Text(
                                "$percentage%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("${state.correctCount} / ${state.totalQuestions} doğru")
                            Text("Süre: ${state.duration / 60} dk ${state.duration % 60} sn")

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(onClick = { viewModel.loadDailyQuiz() }) {
                                Text("Tekrar Çöz")
                            }
                        }
                    }
                }
            }
        }
    }
}
