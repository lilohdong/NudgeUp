package com.nudgeup.app.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.nudgeup.app.R;

public class ToastUtils {

    private static final String TAG = "ToastUtils";

    public static void show(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String message, int duration) {
        if (context == null) return;
        try {
            View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
            ((TextView) layout.findViewById(R.id.tv_toast_message)).setText(message);
            // setView()는 API 30에서 deprecated됐으나, foreground Activity/Fragment 컨텍스트에서는
            // 모든 API 레벨에서 정상 동작함. OS가 foreground 제한을 강화하면 catch fallback이 처리.
            @SuppressWarnings("deprecation")
            Toast toast = new Toast(context);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        } catch (Exception e) {
            Log.e(TAG, "Custom toast failed, falling back to system toast", e);
            Toast.makeText(context, message, duration).show();
        }
    }
}
