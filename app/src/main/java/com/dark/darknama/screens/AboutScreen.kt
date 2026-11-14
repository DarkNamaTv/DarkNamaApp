package com.dark.darknama.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.utils.DeviceUtils
import androidx.compose.foundation.shape.CircleShape

@Composable
fun AboutScreen(navController: NavController?) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with back button like Favorites screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController?.popBackStack() },
                modifier = Modifier.tvFocusItem("about-back", initial = true)
                    .tvFocusBorder(CircleShape)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            
            Text(
                text = stringResource(R.string.about),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(R.drawable.splash_logo)
                    .crossfade(true)
                    .build(),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name
            Text(
                text = "DarkNama",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // App version (matches the release/tag version used by the CI build)
            Text(
                text = stringResource(
                    R.string.app_version,
                    com.dark.darknama.BuildConfig.VERSION_NAME,
                    com.dark.darknama.BuildConfig.VERSION_CODE
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Developer Info
            Text(
                text = "ÐΛɌ₭ᑎΞ𐒡𐒡",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // DarkNama Website Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // DarkNama Banner Image - opens website when clicked
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://raw.githubusercontent.com/DarknamaTv/DarkNama/main/darknama.jpg")
                            .crossfade(true)
                            .build(),
                        contentDescription = "DarkNama Website",
                        modifier = Modifier
                            .tvFocusItem("about-website-banner")
                            .fillMaxWidth()
                            .tvFocusBorder(RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://darknama.pages.dev/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle error if needed
                                }
                            },
                        contentScale = ContentScale.FillWidth
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Website Link Text
                    Text(
                        text = "وبسایت DarkNama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .tvFocusItem("about-website-link")
                            .tvFocusBorder(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://darknama.pages.dev/"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Handle error if needed
                                }
                            }
                            .padding(4.dp)
                    )
                    
                    Text(
                        text = "darknama.pages.dev",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Links Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connect with us",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Download DarkNama from GitHub Releases
                    LinkItem(
                        icon = Icons.Default.Download,
                        trailingIcon = Icons.Default.Code,
                        text = "Download DarkNama",
                        url = "https://github.com/DarknamaTv/DarkNamaApp/releases"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Telegram Channel Link
                    LinkItem(
                        icon = Icons.Default.Send,
                        text = "Telegram Channel",
                        url = "https://t.me/DarkNama_TV"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Attribution also covers optional fonts when the user supplies
            // their original files; do not imply they are bundled by default.
            // TV viewers can navigate to each informational paragraph and
            // scroll it into view even though there are no links below them.
            Text(
                text = stringResource(R.string.about_font_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = attributionFocusModifier(DeviceUtils.isTv(context))
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_english_font_license),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = attributionFocusModifier(DeviceUtils.isTv(context))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_english_font_files),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = attributionFocusModifier(DeviceUtils.isTv(context))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Copyright
            Text(
                text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} DarkNama. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun attributionFocusModifier(isTv: Boolean): Modifier =
    if (isTv) Modifier.fillMaxWidth().tvBringIntoViewOnFocus()
        .tvFocusBorder(RoundedCornerShape(8.dp)).focusable()
    else Modifier.fillMaxWidth()

@Composable
fun LinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    url: String,
    isEmail: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .tvFocusItem("about-link-$url")
            .fillMaxWidth()
            .tvFocusBorder(RoundedCornerShape(8.dp))
            .clickable {
                try {
                    val intent = if (isEmail) {
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$url")
                        }
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Open link",
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}
