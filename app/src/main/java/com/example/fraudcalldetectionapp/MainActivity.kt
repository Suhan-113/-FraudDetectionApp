package com.example.fraudcalldetectionapp

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fraudcalldetectionapp.ui.theme.FraudCallDetectionAppTheme

class MainActivity : ComponentActivity() {

    private val permissionsToRequest = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FraudCallDetectionAppTheme {
                val context = LocalContext.current

                // These states control which screen is shown
                var hasRequiredPermissions by remember { mutableStateOf(hasPermissions(context)) }
                var isServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context, "com.example.fraudcalldetectionapp.CallAnalyzerService")) }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        hasRequiredPermissions = permissions.entries.all { it.value }
                        if (hasRequiredPermissions) {
                            Log.d("Permissions", "All permissions granted. Transitioning to service check.")
                        } else {
                            Log.d("Permissions", "Permissions denied.")
                        }
                    }
                )

                // This DisposableEffect is the key to fixing the state issues.
                // It ensures the state variables are re-evaluated and the UI is recomposed
                // whenever the user returns to the app from settings.
                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val newPermissionsState = hasPermissions(context)
                            val newServiceState = isAccessibilityServiceEnabled(context, "com.example.fraudcalldetectionapp.CallAnalyzerService")

                            // Only update if the state has actually changed to avoid unnecessary recompositions
                            if (hasRequiredPermissions != newPermissionsState) {
                                hasRequiredPermissions = newPermissionsState
                            }
                            if (isServiceEnabled != newServiceState) {
                                isServiceEnabled = newServiceState
                            }
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                if (!hasRequiredPermissions) {
                    PermissionScreen(onAllowClick = {
                        requestPermissionLauncher.launch(permissionsToRequest)
                    })
                } else if (!isServiceEnabled) {
                    ServicePromptScreen(onEnableClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    })
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "App is running and analyzing calls.")
                    }
                }
            }
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.name == serviceName }
    }
}

// Your composables remain the same as they are well-defined
@Composable
fun PermissionScreen(onAllowClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission Required",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "To protect you from fraud calls, we need permission to listen to the call and analyze for scams.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAllowClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(text = "Allow", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ServicePromptScreen(onEnableClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enable Accessibility Service",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "For real-time call analysis, please enable the Fraud Call Detection service in your phone's settings.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(text = "Go to Settings", color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    FraudCallDetectionAppTheme {
        PermissionScreen(onAllowClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ServicePromptScreenPreview() {
    FraudCallDetectionAppTheme {
        ServicePromptScreen(onEnableClick = {})
    }
}