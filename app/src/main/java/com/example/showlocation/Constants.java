package com.example.showlocation;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Constants {

    public static String date;

    public static String messageDurationFormat() {
        String message = "Date: ";
        String date1 = formatDate();
        message += date1 + " ";
        return message;
    }

    public static String messageEndFormat() {
        String message = "Stopping . . . ";
        String date1 = formatDate();
        message += date1 + " ";
        return message;
    }

    public static String formatDate() {
        DateFormat sdf = new SimpleDateFormat("MMM/dd/yyyy", Locale.getDefault());
        date = sdf.format(new Date());
        return date;
    }

    public static String createDurationCounter(int count) {
        String formatDuration = String.format(Locale.getDefault(), "%02d:%02d",
                (count % 3600) / 60, (count % 60));
        return formatDuration;
    }


}
