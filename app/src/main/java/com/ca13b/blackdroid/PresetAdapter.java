package com.ca13b.blackdroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class PresetAdapter extends ArrayAdapter<Preset> {

    public PresetAdapter(Context context, List<Preset> presets) {
        super(context, 0, presets);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_preset, parent, false);
        }

        Preset preset = getItem(position);

        TextView numberView = convertView.findViewById(R.id.text_preset_number);
        TextView nameView = convertView.findViewById(R.id.text_preset_name);

        numberView.setText(String.valueOf(preset.presetNumber));
        nameView.setText(preset.presetName);

        return convertView;
    }
}
