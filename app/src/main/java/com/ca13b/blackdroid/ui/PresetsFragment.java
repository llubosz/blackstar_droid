package com.ca13b.blackdroid.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ca13b.blackdroid.BlackstarAmp;
import com.ca13b.blackdroid.MainActivity;
import com.ca13b.blackdroid.Preset;
import com.ca13b.blackdroid.PresetAdapter;
import com.ca13b.blackdroid.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PresetsFragment extends Fragment implements MainActivity.PresetRefreshListener {

    private static final String tag = "BSD/PresetsFragment";
    private BlackstarAmp amp;
    private PresetAdapter presetAdapter;
    private ArrayList<Preset> presets;
    private ListView listView;
    private TextView headerView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_presets, container, false);

        headerView = root.findViewById(R.id.text_presets_header);
        listView = root.findViewById(R.id.list_presets);
        Button saveButton = root.findViewById(R.id.btn_save_preset);

        amp = MainActivity.blackstarAmp;

        presets = new ArrayList<>();
        presetAdapter = new PresetAdapter(getContext(), presets);
        listView.setAdapter(presetAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Preset preset = presets.get(position);
                if (amp != null && amp.isInitialized) {
                    amp.SelectPreset(preset.presetNumber);
                    Toast.makeText(getContext(),
                            "Loading preset " + preset.presetNumber + ": " + preset.presetName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No amp connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog();
            }
        });

        ((MainActivity) getActivity()).setPresetRefreshListener(this);

        if (amp != null && amp.isInitialized) {
            headerView.setText("Presets — Loading...");
            amp.GetAllPresets();
        } else {
            headerView.setText("Presets — No amp connected");
        }

        return root;
    }

    private void showSaveDialog() {
        if (amp == null || !amp.isInitialized) {
            Toast.makeText(getContext(), "No amp connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Save to Preset");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        final EditText slotInput = new EditText(getContext());
        slotInput.setHint("Slot number (1-128)");
        slotInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(slotInput);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("Preset name (max 20 chars)");
        nameInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String slotText = slotInput.getText().toString().trim();
                String name = nameInput.getText().toString().trim();

                if (slotText.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a slot number", Toast.LENGTH_SHORT).show();
                    return;
                }

                int slot;
                try {
                    slot = Integer.parseInt(slotText);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid slot number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (slot < 1 || slot > 128) {
                    Toast.makeText(getContext(), "Slot must be between 1 and 128", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (name.isEmpty()) {
                    name = "Preset " + slot;
                }
                if (name.length() > 20) {
                    name = name.substring(0, 20);
                }

                amp.SavePreset(slot, name);
                Toast.makeText(getContext(),
                        "Saved preset " + slot + ": " + name,
                        Toast.LENGTH_SHORT).show();

                // Refresh the list entry
                updatePresetInList(slot, name);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updatePresetInList(int presetNumber, String name) {
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).presetNumber == presetNumber) {
                presets.get(i).presetName = name;
                presetAdapter.notifyDataSetChanged();
                return;
            }
        }
        presets.add(new Preset(presetNumber, name));
        presetAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPresetNameReceived(int presetNumber, String name) {
        Log.i(tag, "Received preset name: " + presetNumber + " = " + name);
        updatePresetInList(presetNumber, name);
        headerView.setText("Presets (" + presets.size() + ")");
    }

    @Override
    public void onPresetSettingsReceived(int presetNumber, ByteBuffer packet) {
        Log.i(tag, "Received preset settings for " + presetNumber);
    }

    @Override
    public void onPresetChanged(int presetNumber) {
        Log.i(tag, "Preset changed to " + presetNumber);
        Toast.makeText(getContext(), "Preset changed to " + presetNumber, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        ((MainActivity) getActivity()).setPresetRefreshListener(null);
        super.onDestroyView();
    }
}
