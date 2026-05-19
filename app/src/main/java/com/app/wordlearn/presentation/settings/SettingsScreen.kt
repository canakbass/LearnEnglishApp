package com.app.wordlearn.presentation.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    userName: String = "",
    onLogout: () -> Unit = {},
    onUpdateDisplayName: (String) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    isGuest: Boolean = false
) {
    val settings by viewModel.settings.collectAsState()
    val backupEvent by viewModel.backupEvent.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<android.net.Uri?>(null) }
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) { viewModel.loadSettings() }

    // SAF: kullanıcı yedek dosyasını nereye kaydetmek istediğini seçer.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportTo(it) } }

    // SAF: kullanıcı yedek dosyasını seçer.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportConfirm = it } }

    // Yedek olaylarını snackbar olarak göster; import sonrası ekranı yenile.
    LaunchedEffect(backupEvent) {
        when (val e = backupEvent) {
            is BackupUiEvent.ExportSuccess -> {
                snackbarHostState.showSnackbar("✓ Yedek hazırlandı (${e.wordCount} kelime dahil)")
                viewModel.clearBackupEvent()
            }
            is BackupUiEvent.ImportSuccess -> {
                snackbarHostState.showSnackbar("✓ Veriler içe aktarıldı (${e.wordCount} kelime). Yenileniyor...")
                viewModel.clearBackupEvent()
                // Import sonrası tüm ViewModel'lerin güncel veriyi çekmesi için Activity yeniden başlat.
                if (e.needsRestart) {
                    activity?.recreate()
                }
            }
            is BackupUiEvent.Error -> {
                snackbarHostState.showSnackbar("Hata: ${e.message}")
                viewModel.clearBackupEvent()
            }
            else -> {}
        }
    }

    // Kullanıcı adı değiştirme dialog'u
    if (showChangeNameDialog) {
        AlertDialog(
            onDismissRequest = { showChangeNameDialog = false },
            title = { Text("Kullanıcı Adını Değiştir") },
            text = {
                OutlinedTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = { Text("Yeni kullanıcı adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newDisplayName.isNotBlank()) {
                            onUpdateDisplayName(newDisplayName)
                            showChangeNameDialog = false
                        }
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showChangeNameDialog = false }) { Text("İptal") }
            }
        )
    }

    // Hesap silme dialog'u
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Hesabı Sil") },
            text = {
                Text(
                    "Hesabınız kalıcı olarak silinecek. " +
                        "Tüm ilerlemeniz, quiz geçmişiniz ve kelimeleriniz silinecek. " +
                        "Bu işlem geri alınamaz. Emin misiniz?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    onDeleteAccount()
                }) { Text("Hesabı Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Çıkış Yap") },
            text = { Text("Hesabınızdan çıkış yapmak istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Çıkış Yap", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("İptal") }
            }
        )
    }

    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("Yedeği geri yükle") },
            text = {
                Text(
                    "Bu işlem mevcut kelime listenizi, ilerlemenizi, quiz geçmişinizi ve " +
                        "hikayelerinizi yedek dosyasındakilerle değiştirir. Devam edilsin mi?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = null
                    viewModel.importFrom(uri)
                }) { Text("İçe Aktar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("İptal") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("⚙️ Ayarlar", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            ProfileCard(userName = userName, level = settings.userLevel)
            Spacer(modifier = Modifier.height(16.dp))

            DailyWordCountCard(
                count = settings.dailyNewWordCount,
                onCountChange = viewModel::updateDailyWordCount
            )
            Spacer(modifier = Modifier.height(16.dp))

            LevelCard(
                selected = settings.userLevel,
                onSelect = viewModel::updateUserLevel
            )
            Spacer(modifier = Modifier.height(16.dp))

            DataBackupCard(
                inProgress = backupEvent is BackupUiEvent.InProgress,
                onExport = { exportLauncher.launch(suggestedBackupFileName()) },
                onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hesap yönetimi
            AccountManagementCard(
                isGuest = isGuest,
                onChangeName = {
                    newDisplayName = userName
                    showChangeNameDialog = true
                },
                onDeleteAccount = { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Çıkış Yap", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileCard(userName: String, level: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (userName.isNotBlank()) userName.first().uppercase() else "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (userName.isNotBlank()) userName else "Kullanıcı",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seviye: $level",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DailyWordCountCard(count: Int, onCountChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Günlük Yeni Kelime Sayısı", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$count kelime",
                fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = count.toFloat(),
                onValueChange = { onCountChange(it.toInt()) },
                valueRange = 5f..30f,
                steps = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5", fontSize = 12.sp)
                Text("30", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LevelCard(selected: String, onSelect: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bilgi Düzeyi", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Başlangıç", "Orta", "İleri").forEach { level ->
                    FilterChip(
                        selected = selected == level,
                        onClick = { onSelect(level) },
                        label = { Text(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DataBackupCard(inProgress: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Verilerim", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Kelimelerin, ilerlemen, quiz geçmişin ve hikayelerin tek dosyada — istediğin yere kaydet, başka cihaza taşı.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (inProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onExport,
                    enabled = !inProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dışa Aktar")
                }
                OutlinedButton(
                    onClick = onImport,
                    enabled = !inProgress,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("İçe Aktar")
                }
            }
        }
    }
}

@Composable
private fun AccountManagementCard(
    isGuest: Boolean,
    onChangeName: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hesap Yönetimi", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            if (isGuest) {
                Text(
                    "Misafir hesaplarında kullanıcı adı değiştirme ve silme özelliği bulunmamaktadır.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                OutlinedButton(
                    onClick = onChangeName,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kullanıcı Adını Değiştir")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hesabı Sil")
                }
            }
        }
    }
}

private fun suggestedBackupFileName(): String {
    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
        .format(java.util.Date())
    return "wordlear