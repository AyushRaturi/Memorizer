package com.example.recall

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class RecallWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecallWidgetContent
}

object RecallWidgetContent : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read the current note fresh on every update.
        val note = NotesStore.current(context).ifBlank { "Add your first note in Recall." }
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF171717))
                    .cornerRadius(20.dp)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECALL",
                    style = TextStyle(color = ColorProvider(Color(0xFFAAAAAAL)), fontSize = 11.sp)
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    note,
                    maxLines = 6,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

suspend fun refreshRecallWidgets(context: Context) {
    RecallWidgetContent.updateAll(context)
}
