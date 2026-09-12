package com.example

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FlashlightScreen()
            }
        }
    }
}

@Composable
fun FlashlightScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var hasFlash by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf(50) }
    
    // Flag to keep track if we purposely turned it off
    var intentionallyTurnedOff by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val id = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId = id
            hasFlash = id != null
        } catch (e: Exception) {
            errorMessage = "Camera access failed"
            e.printStackTrace()
        }
    }

    DisposableEffect(cameraManager, cameraId, intentionallyTurnedOff) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    isTorchOn = enabled
                    
                    // The ultimate troll: if the torch turns off (e.g. system turns it off when backgrounded)
                    // but we didn't intentionally turn it off, TURN IT BACK ON!
                    if (!enabled && !intentionallyTurnedOff) {
                        try {
                            cameraManager.setTorchMode(id, true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        cameraManager.registerTorchCallback(callback, null)
        
        // Also force it back on during lifecycle changes if needed
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if ((event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || 
                 event == androidx.lifecycle.Lifecycle.Event.ON_STOP || 
                 event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) && !intentionallyTurnedOff) {
                cameraId?.let {
                    try {
                        cameraManager.setTorchMode(it, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            cameraManager.unregisterTorchCallback(callback)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun toggleTorch() {
        if (isTorchOn) {
            showPremiumDialog = true
        } else {
            cameraId?.let { id ->
                try {
                    cameraManager.setTorchMode(id, true)
                    intentionallyTurnedOff = false
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Could not turn on flashlight"
                    e.printStackTrace()
                }
            }
        }
    }

    fun forceTurnOff() {
        cameraId?.let { id ->
            try {
                intentionallyTurnedOff = true
                cameraManager.setTorchMode(id, false)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Could not turn off flashlight"
                e.printStackTrace()
            }
        }
    }

    val topGradientColor by animateColorAsState(
        targetValue = if (isTorchOn) Color(0xFF2B281B) else Color(0xFF141414),
        label = "topGradientColor"
    )
    
    val bottomGradientColor = Color(0xFF000000)

    val iconBackgroundColor by animateColorAsState(
        targetValue = if (isTorchOn) Color(0xFFFFD54F) else Color(0xFF1E1E1E),
        label = "iconBackgroundColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isTorchOn) Color(0xFF000000) else Color(0xFF666666),
        label = "iconColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1E2124), Color(0xFF121415))))
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (!hasFlash && errorMessage == null) {
                Text(
                    text = "No flashlight detected.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(64.dp))
                    
                    Text(
                        text = "PREMIUM FLASHLIGHT",
                        color = if (isTorchOn) Color(0xFFFFD54F) else Color(0xFF6B7280),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 3D Neumorphic Main Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(280.dp)
                    ) {
                        if (isTorchOn) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD54F).copy(alpha = pulseAlpha))
                            )
                        }

                        // Outer Bevel (Dark ring)
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF101213), Color(0xFF282B30)),
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner Button (Convex)
                            Box(
                                modifier = Modifier
                                    .size(190.dp)
                                    .shadow(
                                        elevation = if (isTorchOn) 24.dp else 4.dp,
                                        shape = CircleShape,
                                        spotColor = if (isTorchOn) Color(0xFFFFD54F) else Color.Black
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = if (isTorchOn) listOf(Color(0xFFFFEA9E), Color(0xFFECAE00)) 
                                                     else listOf(Color(0xFF33373D), Color(0xFF191C1F)),
                                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                    contentDescription = "Flashlight Icon",
                                    tint = if (isTorchOn) Color(0xFF5A3C00) else Color(0xFF0F1112),
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 3D Power Switch / Button
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1A1C1F), Color(0xFF23272B))
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF30353A),
                                shape = RoundedCornerShape(40.dp)
                            )
                            .clickable { toggleTorch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.95f)
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (isTorchOn) listOf(Color(0xFFFFDF73), Color(0xFFF5B200)) 
                                                 else listOf(Color(0xFF32373D), Color(0xFF1C1F23))
                                    )
                                )
                                .shadow(if (isTorchOn) 16.dp else 0.dp, spotColor = Color(0xFFFFD54F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isTorchOn) "TURN OFF" else "TURN ON",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp,
                                color = if (isTorchOn) Color(0xFF6B4700) else Color(0xFF88929C)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(64.dp))
                    
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        
        if (showPremiumDialog) {
            AlertDialog(
                onDismissRequest = { /* Require explicit action */ },
                title = { 
                    Text(
                        text = "Premium Required", 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ) 
                },
                text = { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Turning off the flashlight is a premium feature. Scan the QR Code below to pay $$price and unlock the 'Turn Off' capability.",
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.payment_qr),
                            contentDescription = "Payment QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                },
                containerColor = Color(0xFFFFF1BA),
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { 
                            Toast.makeText(context, "Payment Failed !", Toast.LENGTH_SHORT).show()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD54F),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showPremiumDialog = false 
                        }
                    ) {
                        Text("Cancel", color = Color.DarkGray)
                    }
                }
            )
        }
    }
}
