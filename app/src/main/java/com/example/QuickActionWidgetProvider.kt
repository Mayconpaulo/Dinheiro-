package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class QuickActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.quick_action_widget_layout)

        // PendingIntent for "Novo Gasto" (Launch MainActivity with Gasto type extra)
        val intentGasto = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ADD_TRANSACTION_TYPE", "gasto")
        }
        val pendingGasto = PendingIntent.getActivity(
            context,
            1001,
            intentGasto,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_novo_gasto, pendingGasto)

        // PendingIntent for "Nova Entrada" (Launch MainActivity with Entrada type extra)
        val intentEntrada = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ADD_TRANSACTION_TYPE", "entrada")
        }
        val pendingEntrada = PendingIntent.getActivity(
            context,
            1002,
            intentEntrada,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_nova_entrada, pendingEntrada)

        // Notify the AppWidgetManager of the update
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
