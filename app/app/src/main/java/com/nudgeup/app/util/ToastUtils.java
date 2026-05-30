package com.nudgeup.app.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.nudgeup.app.R;

public class ToastUtils {

    public static void show(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    @SuppressWarnings("deprecation")
    public static void show(Context context, String message, int duration) {
        try {
            View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
            ((TextView) layout.findViewById(R.id.tv_toast_message)).setText(message);
            Toast toast = new Toast(context.getApplicationContext());
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(context, message, duration).show();
        }
    }
}
