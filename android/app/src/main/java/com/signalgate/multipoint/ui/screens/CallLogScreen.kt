package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.multipoint.ui.RecentCallsViewModel
import com.signalgate.multipoint.ui.components.GlassmorphicCard
import com.signalgate.multipoint.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallLogScreen(
    modifier: Modifier = Modifier,
    viewModel: RecentCallsViewModel = koinViewModel()
) {
    val callLogs by viewModel.recentCalls.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "TELEMETRY CALL LOG",
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (callLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent calls", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = callLogs, key = { it.id }) { log ->
                    GlassmorphicCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.phoneNumber, color = TextPrimary, fontSize = 16.sp)
                                Text(
                                    dateFormat.format(Date(log.timestamp)),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                
                                log.matchedSources?.let { sources ->
                                    if (sources.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Matched: $sources",
                                            color = NeonOrange,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val typeColor = when (log.decision) {
                                    "BLOCK" -> NeonRed
                                    "ALLOW" -> NeonGreen
                                    "SCREEN" -> NeonOrange
                                    else -> TextSecondary
                                }
                                Text(log.decision, color = typeColor, fontSize = 14.sp)
                                
                                log.confidence?.let { confidence ->
                                    if (confidence > 0) {
                                        Text("Risk: $confidence%", color = NeonRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
