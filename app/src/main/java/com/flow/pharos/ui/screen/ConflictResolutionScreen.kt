package com.flow.pharos.ui.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flow.pharos.core.model.entity.ClaimEntity
import com.flow.pharos.ui.components.SwipeCard
import com.flow.pharos.ui.viewmodel.ConflictResolutionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionScreen(viewModel: ConflictResolutionViewModel) {
    val conflictClaims by viewModel.conflictClaims.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Conflict Resolution") },
                    actions = {
                        Box(modifier = Modifier.padding(end = 16.dp)) {
                            Text(
                                text = "${conflictClaims.size} conflicts",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                if (conflictClaims.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { 0f }, // No total to track against for now
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (conflictClaims.isEmpty()) {
                        // Empty state
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SSOT is Consistent",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No conflicts remaining. All claims are resolved.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Card stack showing top 2 conflict claims
                        val displayClaims = conflictClaims.take(2).reversed()

                        Box(modifier = Modifier.fillMaxSize()) {
                            displayClaims.forEachIndexed { index, claim ->
                                key(claim.id) {
                                    var existingClaim by remember { mutableStateOf<ClaimEntity?>(null) }
                                    if (index == displayClaims.lastIndex) {
                                        LaunchedEffect(claim) {
                                            existingClaim = viewModel.getConflictingClaim(claim)
                                        }
                                    }

                                    SwipeCard(
                                        newClaim = claim,
                                        existingClaim = existingClaim,
                                        onSwipedLeft = {
                                            viewModel.rejectClaim(claim)
                                        },
                                        onSwipedRight = {
                                            viewModel.approveNewClaim(claim)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom action buttons
                if (conflictClaims.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (conflictClaims.isNotEmpty()) {
                                    viewModel.rejectClaim(conflictClaims.first())
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = "Reject",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        FloatingActionButton(
                            onClick = { viewModel.undoLastSwipe() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (uiState.canUndo)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                if (conflictClaims.isNotEmpty()) {
                                    viewModel.approveNewClaim(conflictClaims.first())
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Approve",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                } else if (uiState.canUndo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.undoLastSwipe() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo")
                        }
                    }
                }
            }
        }
    }
}
