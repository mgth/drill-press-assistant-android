package fr.mgth.drillpress.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.mgth.drillpress.R

private const val REPO_URL = "https://github.com/mgth/drill-press-assistant-android"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun AboutScreen(app: AppState) {
    val t = app.t
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Drill Press Assistant", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text("${t.aboutVersion} $version — GPL-3.0-or-later", color = Color(0xFF6B6B6B), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(t.aboutDescription, style = MaterialTheme.typography.bodyMedium)
            Text("© Mathieu GRENET", color = Color(0xFF6B6B6B), style = MaterialTheme.typography.bodySmall)
        }
    }

    Card {
        Column(Modifier.padding(vertical = 4.dp)) {
            LinkRow(t.aboutSource, REPO_URL)
            LinkRow(t.aboutIssues, "$REPO_URL/issues")
            LinkRow(t.aboutLicense, LICENSE_URL)
        }
    }
}

/**
 * Icône de l'app : fond du launcher + couche foreground. Le vecteur foreground
 * réserve les marges de l'adaptive icon (zone utile 66/108), d'où le
 * requiredSize surdimensionné (64 × 108/66) rogné par le clip.
 */
@Composable
private fun AppIcon() {
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF263238)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(105.dp),
        )
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text("↗", color = MaterialTheme.colorScheme.primary)
    }
}
