package com.signalgate.multipoint.ui.onboarding

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

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
fun PermissionsStep(navController: NavHostController) {
    Button(onClick = { navController.navigate("contacts") }) {
        Text("Grant Permissions & Continue")
    }
}

@Composable
fun ContactsImportStep(navController: NavHostController) {
    // Auto-import all contacts + multi-select UI
    Button(onClick = { navController.navigate("sources") }) {
        Text("Import Contacts (Auto-Allow) & Continue")
    }
}

@Composable
fun SourcesSelectionStep(navController: NavHostController) {
    Button(onClick = { navController.navigate("risk") }) {
        Text("Select Sources & Continue")
    }
}

@Composable
fun RiskThresholdStep(navController: NavHostController) {
    Button(onClick = { /* Finish onboarding */ }) {
        Text("Set Risk Threshold & Finish")
    }
}
