package com.apticstudio.scratchcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apticstudio.scratchcard.ui.theme.ScratchCardTheme

/**
 * Data class representing reward data fetched from a simulated database or network API.
 */
data class RewardData(
    val id: String,
    val title: String,
    val amount: String,
    val promoCode: String,
    val accentColor: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScratchCardTheme {
                ScratchCardSampleApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchCardSampleApp() {
    val scratchState = rememberScratchCardState()

    // Simulated local SQLite database or remote API reward response
    var rewardData by remember {
        mutableStateOf(
            RewardData(
                id = "REW-88392",
                title = "Jackpot Prize Won!",
                amount = "$100 CASHBACK",
                promoCode = "LUCKY2026",
                accentColor = Color(0xFF4CAF50)
            )
        )
    }

    var scratchStatusText by remember { mutableStateOf("Scratch the entire card to reveal!") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScratchCard Library Sample", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = scratchStatusText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cleared: ${(scratchState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(90.dp)
                        )
                        LinearProgressIndicator(
                            progress = { scratchState.progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Scratch Card Surface
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                ScratchCard(
                    modifier = Modifier.fillMaxSize(),
                    state = scratchState,
                    overlayColor = Color(0xFFB0BEC5),
                    overlayText = "✨ SCRATCH HERE TO REVEAL ✨",
                    scratchThickness = 36.dp,
                    clearThreshold = 0.95f, // Requires complete scratching (95%+)
                    autoClearOnThreshold = false, // By default, requires full manual scratching
                    onScratchStarted = {
                        scratchStatusText = "Scratching started..."
                    },
                    onScratchProgress = { progress ->
                        if (progress < 0.95f) {
                            scratchStatusText = "Scratch more to reveal completely! (${(progress * 100).toInt()}%)"
                        }
                    },
                    onScratchCompleted = {
                        scratchStatusText = "🎉 Congratulations! Full Scratch Completed!"
                    }
                ) {
                    // Simulated Database / Network Content Slot
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA000)
                                    )
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Reward Icon",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF3E2723)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rewardData.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723)
                            )
                            Text(
                                text = rewardData.amount,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PROMO: ${rewardData.promoCode}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        scratchState.reset()
                        scratchStatusText = "Card reset. Scratch completely to reveal!"
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Card")
                }

                OutlinedButton(
                    onClick = {
                        scratchState.clear()
                        scratchStatusText = "Card programmatically revealed!"
                    }
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "Reveal")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reveal All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulate Network / DB Reload
            OutlinedButton(
                onClick = {
                    val rewards = listOf(
                        RewardData("REW-101", "Special Offer!", "$25 GIFT CARD", "SAVE25", Color(0xFF2196F3)),
                        RewardData("REW-202", "Super Winner!", "$50 VOUCHER", "SUPER50", Color(0xFFE91E63)),
                        RewardData("REW-303", "Mega Jackpot!", "$500 CASH", "MEGA500", Color(0xFF9C27B0))
                    )
                    rewardData = rewards.random()
                    scratchState.reset()
                    scratchStatusText = "Loaded new database reward! Scratch to reveal."
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = "Fetch New Data")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fetch New Reward Data")
            }
        }
    }
}
