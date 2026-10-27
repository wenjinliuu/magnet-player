package com.wenjin.magnetplayer.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenjin.magnetplayer.ui.theme.Accent

/** 48×28 开关（设计稿样式），可点击区域通过外层行保证 ≥ 44dp */
@Composable
fun MagnetSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onColor: Color,
  offColor: Color,
  modifier: Modifier = Modifier,
) {
  val thumbOffset by animateDpAsState(if (checked) 23.dp else 3.dp, tween(150), label = "thumb")
  Box(
    modifier
      .size(48.dp, 28.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(if (checked) onColor else offColor)
      .clickable(role = Role.Switch) { onCheckedChange(!checked) },
  ) {
    Box(
      Modifier
        .offset(x = thumbOffset, y = 3.dp)
        .size(22.dp)
        .shadow(1.5.dp, CircleShape)
        .background(Color.White, CircleShape),
    )
  }
}

/** 彩色小标签，例如"4 个文件"、"字幕" */
@Composable
fun Tag(text: String, accent: Accent, modifier: Modifier = Modifier) {
  Text(
    text,
    modifier
      .background(accent.tint, RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    color = accent.onTint,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
  )
}

/** 实色圆角图标块 */
@Composable
fun IconBlock(
  icon: ImageVector,
  accent: Accent,
  size: Dp = 40.dp,
  iconSize: Dp = 20.dp,
  corner: Dp = 12.dp,
) {
  Box(
    Modifier
      .size(size)
      .background(accent.fill, RoundedCornerShape(corner)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, null, Modifier.size(iconSize), tint = accent.onFill)
  }
}

/** 浅底按钮：图标 + 文字（粘贴、种子文件） */
@Composable
fun TintButton(
  text: String,
  icon: ImageVector,
  accent: Accent,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier
      .clip(RoundedCornerShape(14.dp))
      .background(accent.tint)
      .clickable(onClick = onClick)
      .padding(PaddingValues(horizontal = 18.dp)),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(icon, null, Modifier.size(18.dp), tint = accent.onTint)
    Text(text, color = accent.onTint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
  }
}
