package com.englishworder.ui.wordlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.englishworder.ui.components.AppCard
import com.englishworder.ui.components.GreenGradientBackground
import com.englishworder.ui.components.ScreenPadding
import com.englishworder.ui.theme.AppColors
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    listId: Long = 0,
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val wordLists by viewModel.wordLists.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(wordLists) {
        if (state.selectedListId == 0L && wordLists.isNotEmpty()) {
            viewModel.setSelectedListId(if (listId > 0) listId else wordLists.first().id)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else "import.csv"
            } ?: "import.csv"
            viewModel.importFile(listId, uri, name)
        }
    }

    GreenGradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("批量导入") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            ScreenPadding(Modifier.padding(padding)) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("导入到", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth().selectable(
                                selected = state.target == ImportTarget.NEW_GROUP,
                                onClick = { viewModel.setTarget(ImportTarget.NEW_GROUP) },
                                role = Role.RadioButton
                            )) {
                                RadioButton(selected = state.target == ImportTarget.NEW_GROUP,
                                    onClick = { viewModel.setTarget(ImportTarget.NEW_GROUP) })
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text("创建新分组")
                                    if (state.target == ImportTarget.NEW_GROUP) {
                                        OutlinedTextField(
                                            value = state.newGroupName,
                                            onValueChange = viewModel::setNewGroupName,
                                            label = { Text("新分组名称") },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().selectable(
                                selected = state.target == ImportTarget.MERGE_EXISTING,
                                onClick = { viewModel.setTarget(ImportTarget.MERGE_EXISTING) },
                                role = Role.RadioButton
                            )) {
                                RadioButton(selected = state.target == ImportTarget.MERGE_EXISTING,
                                    onClick = { viewModel.setTarget(ImportTarget.MERGE_EXISTING) })
                                Text("并入已有分组", modifier = Modifier.padding(start = 8.dp, top = 12.dp))
                            }
                            if (state.target == ImportTarget.MERGE_EXISTING) {
                                wordLists.forEach { list ->
                                    FilterChip(
                                        selected = state.selectedListId == list.id,
                                        onClick = { viewModel.setSelectedListId(list.id) },
                                        label = { Text("${list.name} (${list.wordCount})") },
                                        modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    AppCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("格式说明", fontWeight = FontWeight.Bold)
                            Text("• 支持 CSV / Excel（.xlsx / .xls）")
                            Text("• 推荐表头：单词、音标、释义、例句原文、例句翻译")
                            Text("• 释义可写「n. 建议，忠告」——词性会自动拆出，释义完整保留")
                            Text("• 表格里的释义、例句、词性优先于自动查词典")
                            Text("• 表格没有的字段才会自动补全")
                        }
                    }

                    Button(
                        onClick = {
                            filePicker.launch(arrayOf("text/csv", "text/comma-separated-values",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.heroGreen)
                    ) { Text("选择文件导入") }

                    OutlinedButton(onClick = {
                        val template = "单词,音标,释义,例句原文,例句翻译\ncounsel,/'kaʊnsl/,n. 建议，忠告；商议,Blessed is the man who walks not in the counsel of the wicked...,不从恶人的计谋…\n"
                        val file = File(context.cacheDir, "word_template.csv")
                        file.writeText(template)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "导出模板"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("导出模板") }

                    if (state.isLoading) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppColors.heroGreen)
                            Text("正在导入...", modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    state.result?.let { result ->
                        AppCard {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("导入完成", fontWeight = FontWeight.Bold, color = AppColors.heroGreen)
                                Text("总计: ${result.total} · 新增: ${result.imported} · 更新: ${result.updated} · 跳过: ${result.skipped}")
                                Text("待补全中文释义: ${result.pendingFetch}")
                            }
                        }
                    }
                    state.fetchProgress?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.result?.pendingFetch?.let { it > 0 } == true) {
                        OutlinedButton(onClick = { viewModel.retryFetch() }) { Text("重试补全") }
                    }
                }
            }
        }
    }
}
