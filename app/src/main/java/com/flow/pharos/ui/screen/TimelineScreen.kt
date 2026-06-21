package com.flow.pharos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.pharos.core.model.ClaimStatus
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.ui.viewmodel.TimelineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel) {
    val claims by viewModel.filteredClaims.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val statistics by viewModel.claimStatistics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claims Timeline") },
                actions = {
                    val totalCount = statistics.values.sum()
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Text(
                            text = "$totalCount claims",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All") }
                )
                ClaimStatus.entries.forEach { status ->
                    val count = statistics[status] ?: 0
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = {
                            viewModel.setStatusFilter(
                                if (statusFilter == status) null else status
                            )
                        },
                        label = { Text("${status.name} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = statusColor(status).copy(alpha = 0.2f)
                        )
                    )
                }
            }

            if (claims.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No claims found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(claims, key = { it.id }) { claim ->
                        ClaimTimelineItem(claim = claim)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimTimelineItem(claim: ClaimEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip
                StatusChip(status = claim.status)
                // Extraction timestamp
                Text(
                    text = dateFormat.format(Date(claim.extractedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Claim content
            Text(
                text = claim.content,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = claim.sourceFileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Source: ${dateFormat.format(Date(claim.sourceTimestamp))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Provenance chain
            if (claim.supersededById != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Superseded by: ${claim.supersededById}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (claim.supersedes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Supersedes: ${claim.supersedes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: ClaimStatus) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun statusColor(status: ClaimStatus): Color = when (status) {
    ClaimStatus.APPROVED -> Color(0xFF4CAF50)
    ClaimStatus.REJECTED -> Color(0xFFF44336)
    ClaimStatus.CONFLICT -> Color(0xFFFF9800)
    ClaimStatus.SUPERSEDED -> Color(0xFF9E9E9E)
    ClaimStatus.PENDING -> Color(0xFF2196F3)
}
