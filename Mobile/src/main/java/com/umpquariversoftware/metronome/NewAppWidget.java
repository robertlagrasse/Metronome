package com.umpquariversoftware.metronome;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {
    private static final String WIDGET_RECEIVER_INTENT = "com.umpquariversoftware.metronome.STARTSTOP";


    // This acts like a list adapter/viewholder
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {

        Log.e("NewAppWidget", "called updateAppWidget()");
        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        remoteViews.setTextViewText(R.id.widgetText, "Jam Name");
        remoteViews.setOnClickPendingIntent(R.id.widgetButton, getPendingSelfIntent(context, true));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

    }

    // This just loops through the individual widgets and sends them to updateAppWidget
    // When an update request is sent
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.e("Widget", "onUpdate called");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

    }

    //This is the actual broadcast receiver for the Widgets. Send something here through
    // the APPWIDGET_UPDATE intent, and this gets kicked off
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.e("NewAppWidget", "onReceive()");
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.new_app_widget);
        views.setTextViewText(R.id.widgetText, intent.getStringExtra("jamName"));
        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, NewAppWidget.class), views);

    }

    @Override
    public void onEnabled(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.new_app_widget);
        views.setTextViewText(R.id.widgetText, "initializing...");
        views.setImageViewResource(R.id.widgetButton, R.drawable.chevron_right);

        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, NewAppWidget.class), views);

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static PendingIntent getPendingSelfIntent(Context context, @SuppressWarnings("SameParameterValue") Boolean fab) {
        Intent intent = new Intent(WIDGET_RECEIVER_INTENT);
        intent.putExtra("widget", true);
        intent.putExtra("fab", fab);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

}

