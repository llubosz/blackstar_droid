package com.ca13b.blackdroid.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ca13b.blackdroid.BlackstarAmp;
import com.ca13b.blackdroid.Control;
import com.ca13b.blackdroid.MainActivity;
import com.ca13b.blackdroid.R;
import com.google.android.material.slider.Slider;

public class ControlsFragment extends Fragment {
    private BlackstarAmp amp;
    private Slider slVolume;
    private Slider slGain;
    private Slider slBass;
    private Slider slMid;
    private Slider slTreble;
    private Slider slIsf;
    private AutoCompleteTextView voiceDropdown;
    Control ctrlGain;
    Control ctrlVolume;
    Control ctrlBass;
    Control ctrlMid;
    Control ctrlTreble;
    Control ctrlVoice;
    Control ctrlIsf;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        amp = MainActivity.blackstarAmp;

        View root = inflater.inflate(R.layout.fragment_controls, container, false);

        initializeControls(root);

        return root;
    }

    private void initializeControls(final View root) {
        ctrlVoice = amp.Controls.get(1);
        ctrlGain = amp.Controls.get(2);
        ctrlVolume = amp.Controls.get(3);
        ctrlBass = amp.Controls.get(4);
        ctrlMid = amp.Controls.get(5);
        ctrlTreble = amp.Controls.get(6);
        ctrlIsf = amp.Controls.get(7);

        slVolume = root.findViewById(R.id.volume_slider);
        slVolume.addOnChangeListener(sliderChanged);
        slGain = root.findViewById(R.id.gain_slider);
        slGain.addOnChangeListener(sliderChanged);
        slBass = root.findViewById(R.id.bass_slider);
        slBass.addOnChangeListener(sliderChanged);
        slMid = root.findViewById(R.id.middle_slider);
        slMid.addOnChangeListener(sliderChanged);
        slTreble = root.findViewById(R.id.treble_slider);
        slTreble.addOnChangeListener(sliderChanged);
        slIsf = root.findViewById(R.id.isf_slider);
        slIsf.addOnChangeListener(sliderChanged);

        voiceDropdown = root.findViewById(R.id.voice_list);
        String[] voiceTypes = getResources().getStringArray(R.array.voice_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, voiceTypes);
        voiceDropdown.setAdapter(adapter);
        voiceDropdown.setOnItemClickListener((parent, view, position, id) ->
                amp.SetControlValue(ctrlVoice, position));

        getInitialValues();
    }

    private void getInitialValues() {
        slVolume.setValue(ctrlVolume.controlValue);
        slGain.setValue(ctrlGain.controlValue);
        slBass.setValue(ctrlBass.controlValue);
        slMid.setValue(ctrlMid.controlValue);
        slTreble.setValue(ctrlTreble.controlValue);
        slIsf.setValue(ctrlIsf.controlValue);

        String[] voiceTypes = getResources().getStringArray(R.array.voice_types);
        if (ctrlVoice.controlValue >= 0 && ctrlVoice.controlValue < voiceTypes.length) {
            voiceDropdown.setText(voiceTypes[ctrlVoice.controlValue], false);
        }
    }

    private final Slider.OnChangeListener sliderChanged = (slider, value, fromUser) -> {
        if (!fromUser) return;
        int progress = (int) value;
        Control ctrlTemp = null;

        int id = slider.getId();
        if (id == R.id.gain_slider) {
            ctrlTemp = ctrlGain;
        } else if (id == R.id.volume_slider) {
            ctrlTemp = ctrlVolume;
        } else if (id == R.id.bass_slider) {
            ctrlTemp = ctrlBass;
        } else if (id == R.id.middle_slider) {
            ctrlTemp = ctrlMid;
        } else if (id == R.id.treble_slider) {
            ctrlTemp = ctrlTreble;
        } else if (id == R.id.isf_slider) {
            ctrlTemp = ctrlIsf;
        }

        if (ctrlTemp == null) return;
        ctrlTemp.controlValue = progress;
        amp.SetControlValue(ctrlTemp, (progress * ctrlTemp.maxValue) / 100);
    };

    @Override
    public void onDestroy() {
        this.amp = null;
        super.onDestroy();
    }
}
