package com.nudgeup.app.ui.main.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.nudgeup.app.R;

public class CreateTeamDialog extends DialogFragment {

    public interface OnCreateTeamListener {
        void onCreateTeam(String teamName);
    }

    private final OnCreateTeamListener listener;

    public CreateTeamDialog(OnCreateTeamListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_create_team, container, false);

        EditText etTeamName = view.findViewById(R.id.et_team_name);
        Button btnCreate = view.findViewById(R.id.btn_create);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnCreate.setOnClickListener(v -> {
            String name = etTeamName.getText().toString().trim();
            if (!name.isEmpty()) {
                listener.onCreateTeam(name);
                dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
        return view;
    }
}
