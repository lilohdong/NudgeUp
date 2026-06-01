package com.nudgeup.app.util;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.nudgeup.app.R;

public class ToastUtils {

    public static void show(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, String message, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: setView()가 deprecated되어 background에서 무음 실패하므로 기본 Toast 사용
            Toast.makeText(context, message, duration).show();
        } else {
            // API 30 미만: 커스텀 뷰 Toast 유지
            try {
                View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
                ((TextView) layout.findViewById(R.id.tv_toast_message)).setText(message);
                @SuppressWarnings("deprecation")
                Toast toast = new Toast(context.getApplicationContext());
                toast.setDuration(duration);
                //noinspection deprecation
                toast.setView(layout);
                toast.show();
            } catch (Exception e) {
                Toast.makeText(context, message, duration).show();
            }
        }
    }
}
