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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.wordlearn.domain.model.LetterResult
import com.app.wordlearn.domain.model.WordleGameState
import com.app.wordlearn.presentation.components.LoadingScreen
import com.app.wordlearn.presentation.theme.Error
import com.app.wordlearn.presentation.theme.Success
import com.app.wordlearn.presentation.theme.WordleAbsent
import com.app.wordlearn.presentation.theme.WordleCorrect
import com.app.wordlearn.presentation.theme.WordleEmpty
import com.app.wordlearn.presentation.theme.WordlePresent

/**
 * Wordle ana ekranı. Cognitive complexity'yi düşük tutmak için
 * grid / game-over kart / klavye 3 ayrı composable'a bölündü.
 */
@Composable
fun WordleScreen(viewModel: WordleViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.startNewGame() }

    if (isLoading) {
        LoadingScreen()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Wordle",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        WordleGrid(gameState)

        Spacer(modifier = Modifier.height(16.dp))

        if (gameState.isGameOver) {
            WordleGameOverCard(gameState, onNewGame = viewModel::startNewGame)
        }

        Spacer(modifier = Modifier.weight(1f))

        WordleKeyboard(
            gameState = gameState,
            onKeyPress = viewModel::onKeyPress,
            onSubmit = viewModel::onSubmit,
            onBackspace = viewModel::onBackspace
        )
    }
}

@Composable
private fun WordleGrid(gameState: WordleGameState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until 6) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 5) {
                    val (letter, bgColor) = cellContent(gameState, row, col)
                    WordleCell(letter = letter, bgColor = bgColor)
                }
            }
        }
    }
}

/** Tek bir grid hücresi için (harf, renk) belirler. */
private fun cellContent(state: WordleGameState, row: Int, col: Int): Pair<String, Color> {
    return when {
        row < state.attempts.size -> {
            val letter = state.attempts[row][col].toString()
            val color = state.guessResults[row][col].toCellColor()
            letter to color
        }
        row == state.attempts.size ->
            (state.currentGuess.getOrNull(col)?.toString() ?: "") to Color.Transparent
        else -> "" to Color.Transparent
    }
}

private fun LetterResult.toCellColor(): Color = when (this) {
    LetterResult.CORRECT -> WordleCorrect
    LetterResult.PRESENT -> WordlePresent
    LetterResult.ABSENT -> WordleAbsent
    LetterResult.UNUSED -> WordleEmpty
}

@Composable
private fun WordleCell(letter: String, bgColor: Color) {
    val isEmpty = bgColor == Color.Transparent
    Box(
        modifier = Modifier
            .size(56.dp)
            .border(2.dp, if (isEmpty) WordleEmpty else bgColor, RoundedCornerShape(4.dp))
            .background(bgColor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEmpty) MaterialTheme.colorScheme.onSurface else Color.White
        )
    }
}

@Composable
private fun WordleGameOverCard(gameState: WordleGameState, onNewGame: () -> Unit) {
    val containerColor = if (gameState.isWon) Success.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (gameState.isWon) "Tebrikler! 🎉" else "Kaybettiniz!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (!gameState.isWon) {
                Text("Doğru kelime: ${gameState.targetWord}", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onNewGame) { Text("Yeni Oyun") }
        }
    }
}

private val KEYBOARD_ROWS = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

@Composable
private fun WordleKeyboard(
    gameState: WordleGameState,
    onKeyPress: (Char) -> Unit,
    onSubmit: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        KEYBOARD_ROWS.forEach { row ->
            val isBottomRow = row == "ZXCVBNM"
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBottomRow) ActionKey(label = "GO", weight = 1.5f, onClick = onSubmit)
                row.forEach { char ->
                    LetterKey(
                        char = char,
                        keyState = gameState.keyboardState[char],
                        onKeyPress = onKeyPress
                    )
                }
                if (isBottomRow) ActionKey(label = "⌫", weight = 1.5f, onClick = onBackspace)
            }
        }
    }
}

@Composable
private fun RowScope.LetterKey(
    char: Char,
    keyState: LetterResult?,
    onKeyPress: (Char) -> Unit
) {
    val keyColor = keyState?.toCellColor() ?: MaterialTheme.colorScheme.surfaceVariant
    Button(
        onClick = { onKeyPress(char) },
        modifier = Modifier.height(48.dp).weight(1f),
        contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = keyColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            char.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (keyState != null) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RowScope.ActionKey(label: String, weight: Float, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp).weight(weight),
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
