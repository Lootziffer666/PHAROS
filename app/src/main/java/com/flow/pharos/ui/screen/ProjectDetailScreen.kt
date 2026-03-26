package com.flow.pharos.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.flow.pharos.core.llm.JsonParser
import com.flow.pharos.util.ContradictionDetector
import com.flow.pharos.util.ProjectRelationGraph
import com.flow.pharos.util.RelationGraphBuilder
import com.flow.pharos.ui.viewmodel.ProjectsViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(projectId: String, viewModel: ProjectsViewModel, onBack: () -> Unit) {
    val projects by viewModel.projects.collectAsState()
    val projectFiles by viewModel.projectFiles.collectAsState()
    val fileAnalyses by viewModel.fileAnalyses.collectAsState()
    val project = projects.find { it.id == projectId }
    val analysesByFileName = remember(projectFiles, fileAnalyses) {
        projectFiles.mapNotNull { file ->
            fileAnalyses[file.id]?.let { analysis -> file.name to analysis }
        }.toMap()
    }
    val contradictionInsights = remember(analysesByFileName) {
        ContradictionDetector.detectPotentialContradictions(analysesByFileName)
    }
    val relationGraph = remember(analysesByFileName) {
        RelationGraphBuilder.build(analysesByFileName)
    }

    LaunchedEffect(projectId) { viewModel.loadProjectFiles(projectId) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(project?.name ?: "Project") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } })

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            project?.let { p ->
                item {
                    Text(p.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${projectFiles.size} files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (contradictionInsights.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFFFFF4E5)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Potential contradictions (${contradictionInsights.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    "Heuristic check based on analysis summaries. Please verify manually.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                contradictionInsights.take(5).forEach { insight ->
                                    Text(
                                        "• ${insight.leftFileName} ↔ ${insight.rightFileName} " +
                                            "(topic: ${insight.sharedTopic})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (relationGraph.edges.isNotEmpty()) {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Relation graph canvas",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Files are blue nodes, topics are green nodes. Lines show file-topic links.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RelationGraphCanvas(
                                    graph = relationGraph,
                                    modifier = Modifier.fillMaxWidth().height(240.dp)
                                )
                                Text(
                                    "Top topics: ${relationGraph.topicNodes.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            items(projectFiles) { file ->
                val analysis = fileAnalyses[file.id]
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall)
                        analysis?.let { a ->
                            Text(a.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val topics = JsonParser.fromJsonArray(a.topics)
                            if (topics.isNotEmpty()) Text("Topics: ${topics.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                        } ?: Text("Not yet analyzed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationGraphCanvas(graph: ProjectRelationGraph, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val leftCenter = Offset(w * 0.28f, centerY)
        val rightCenter = Offset(w * 0.72f, centerY)
        val fileRadius = (h * 0.34f).coerceAtLeast(40f)
        val topicRadius = (h * 0.34f).coerceAtLeast(40f)

        val filePositions = graph.fileNodes.mapIndexed { index, name ->
            val angle = 2 * PI * index / graph.fileNodes.size.coerceAtLeast(1)
            name to Offset(
                (leftCenter.x + fileRadius * cos(angle).toFloat()).coerceIn(30f, w - 30f),
                (leftCenter.y + fileRadius * sin(angle).toFloat()).coerceIn(24f, h - 24f)
            )
        }.toMap()

        val topicPositions = graph.topicNodes.mapIndexed { index, topic ->
            val angle = 2 * PI * index / graph.topicNodes.size.coerceAtLeast(1)
            topic to Offset(
                (rightCenter.x + topicRadius * cos(angle).toFloat()).coerceIn(30f, w - 30f),
                (rightCenter.y + topicRadius * sin(angle).toFloat()).coerceIn(24f, h - 24f)
            )
        }.toMap()

        graph.edges.forEach { (file, topic) ->
            val p1 = filePositions[file]
            val p2 = topicPositions[topic]
            if (p1 != null && p2 != null) {
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = p1,
                    end = p2,
                    strokeWidth = 2f
                )
            }
        }

        filePositions.values.forEach { p ->
            drawCircle(color = Color(0xFF2563EB), radius = 10f, center = p)
            drawCircle(color = Color.White, radius = 10f, center = p, style = Stroke(1f))
        }
        topicPositions.values.forEach { p ->
            drawCircle(color = Color(0xFF059669), radius = 9f, center = p)
            drawCircle(color = Color.White, radius = 9f, center = p, style = Stroke(1f))
        }
    }
}
