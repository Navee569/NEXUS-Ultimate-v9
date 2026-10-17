package com.nexus.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Original NEXUS design tokens. Components intentionally avoid expensive blur and perpetual motion. */
object NexusPalette {
  val Amoled = Color(0xFF000000)
  val Elevated = Color(0xFF0B1014)
  val Ice = Color(0xFF72E5F2)
  val Muted = Color(0xFF9BB2C0)
  val Text = Color(0xFFF1F7FA)
  val Danger = Color(0xFFFF6B78)
}

object NexusShape {
  val Card = RoundedCornerShape(18.dp)
  val Control = RoundedCornerShape(12.dp)
}

@Composable
fun NexusSectionHeader(title: String, modifier: Modifier = Modifier) {
  Text(
    text = title,
    modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurface,
  )
}

@Composable
fun NexusInfoCard(
  title: String,
  body: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = NexusShape.Card,
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun NexusMetadataRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().background(Color.Transparent).padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
  }
}
