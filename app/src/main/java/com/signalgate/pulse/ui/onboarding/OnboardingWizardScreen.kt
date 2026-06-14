// Jetpack Compose Onboarding Wizard for Pulse
// Includes contacts auto-allow and risk threshold

package com.signalgate.multipoint.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingWizardScreen(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") { PermissionsStep(navController) }
        composable("contacts") { ContactsImportStep(navController) }
        composable("sources") { SourcesSelectionStep(navController) }
        composable("risk") { RiskThresholdStep(navController) }
    }
}

@Composable
fun PermissionsStep(
    navController: NavHostController,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val permissionStates by viewModel.permissionStates.collectAsState()

    // Request all permissions in one shot
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, granted) ->
            viewModel.onPermissionResult(permission, granted)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SignalGate needs the following permissions to protect your calls. Required permissions must be granted to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.permissions) { item ->
                    val granted = permissionStates[item.permission] == true
                    PermissionRow(
                        title = item.title,
                        rationale = item.rationale,
                        isRequired = item.isRequired,
                        isGranted = granted
                    )
                }
            }
        }

        Column {
            Button(
                onClick = {
                    launcher.launch(
                        viewModel.permissions.map { it.permission }.toTypedArray()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permissions")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (viewModel.allRequiredGranted()) {
                        navController.navigate("contacts")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.allRequiredGranted()
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    rationale: String,
    isRequired: Boolean,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (isRequired) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = if (isGranted) "✓" else "○",
            color = if (isGranted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ContactsImportStep(navController: NavHostController) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        navController.navigate("sources")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SignalGate can automatically allow calls from people in your contacts. This is optional — you can change it later in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            Button(
                onClick = {
                    launcher.launch(Manifest.permission.READ_CONTACTS)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Contacts Access")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate("sources") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
fun SourcesSelectionStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Data Sources",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SignalGate uses community-maintained block lists to identify spam and scam callers. You can manage sources later in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = { navController.navigate("risk") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun RiskThresholdStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Risk Threshold",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "How aggressively should SignalGate block calls? You can fine-tune this later in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = { /* Mark onboarding complete, navigate to main */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish Setup")
        }
    }
}
l
