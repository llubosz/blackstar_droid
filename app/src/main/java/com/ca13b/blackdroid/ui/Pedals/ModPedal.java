package com.ca13b.blackdroid.ui.Pedals;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ca13b.blackdroid.BlackstarAmp;
import com.ca13b.blackdroid.Control;
import com.ca13b.blackdroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class ModPedal {

    private AutoCompleteTextView modTypeList;
    private Slider slModSeqVal;
    private Slider slModDepth;
    private Slider slModSpeed;
    private Slider slModManual;

    private MaterialButton modPowerSwitch;
    private View modPowerLed;
    public Control ctrlModType;
    public Control ctrlModSeqVal;
    public Control ctrlModDepth;
    public Control ctrlModSpeed;
    public Control ctrlModManual;
    public Control ctrlModPower;

    public TextView seqval_label;
    public TextView manual_label;
    public TextView pedal_label;

    boolean modPowerOn;
    BlackstarAmp amp;

    public void InitializeControls(final Context context,
                                   final BlackstarAmp amp,
                                   final View root,
                                   final Slider.OnChangeListener sliderChanged) {

        this.amp = amp;
        this.modPowerOn = false;

        ctrlModType = amp.Controls.get(18);
        ctrlModDepth = amp.Controls.get(21);
        ctrlModSeqVal = amp.Controls.get(19);
        ctrlModSpeed = amp.Controls.get(22);
        ctrlModManual = amp.Controls.get(20);
        ctrlModPower = amp.Controls.get(15);

        seqval_label = root.findViewById(R.id.seqval_label);
        pedal_label = root.findViewById(R.id.pedal_label);
        manual_label = root.findViewById(R.id.manual_label);

        modTypeList = root.findViewById(R.id.mod_type_list);
        String[] modTypes = context.getResources().getStringArray(R.array.mod_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, modTypes);
        modTypeList.setAdapter(adapter);
        modTypeList.setOnItemClickListener((parent, view, position, id) -> {
            updateModTypeUI(position);
            amp.SetControlValue(ctrlModType, position);
        });

        slModSeqVal = root.findViewById(R.id.mod_seqval_slider);
        slModSeqVal.addOnChangeListener(sliderChanged);

        slModDepth = root.findViewById(R.id.mod_depth_slider);
        slModDepth.addOnChangeListener(sliderChanged);

        slModSpeed = root.findViewById(R.id.mod_speed_slider);
        slModSpeed.addOnChangeListener(sliderChanged);

        slModManual = root.findViewById(R.id.mod_manual_slider);
        slModManual.addOnChangeListener(sliderChanged);

        modPowerLed = root.findViewById(R.id.mod_power_led);

        modPowerSwitch = root.findViewById(R.id.mod_power_switch);
        modPowerSwitch.setOnClickListener(v -> {
            modPowerOn = !modPowerOn;
            ctrlModPower.controlValue = modPowerOn ? 1 : 0;
            modPowerLed.setBackground(ContextCompat.getDrawable(context,
                    modPowerOn ? R.drawable.led_on : R.drawable.led_off));
            amp.SetControlValue(ctrlModPower, ctrlModPower.controlValue);
        });

        getInitialValues(context);
    }

    private void getInitialValues(Context context) {
        slModDepth.setValue(ctrlModDepth.controlValue);
        slModSeqVal.setValue(ctrlModSeqVal.controlValue);
        slModManual.setValue(ctrlModManual.controlValue);
        slModSpeed.setValue(ctrlModSpeed.controlValue);

        String[] modTypes = context.getResources().getStringArray(R.array.mod_types);
        if (ctrlModType.controlValue >= 0 && ctrlModType.controlValue < modTypes.length) {
            modTypeList.setText(modTypes[ctrlModType.controlValue], false);
        }

        modPowerOn = ctrlModPower.controlValue == 1;
        modPowerLed.setBackground(ContextCompat.getDrawable(context,
                modPowerOn ? R.drawable.led_on : R.drawable.led_off));

        updateModTypeUI(ctrlModType.controlValue);
    }

    private void updateModTypeUI(int typeValue) {
        switch (typeValue) {
            case 0:
                pedal_label.setText("PHASER");
                seqval_label.setText("MIX");
                manual_label.setVisibility(View.GONE);
                slModManual.setVisibility(View.GONE);
                break;
            case 1:
                pedal_label.setText("FLANGER");
                seqval_label.setText("FEEDBACK");
                manual_label.setVisibility(View.VISIBLE);
                slModManual.setVisibility(View.VISIBLE);
                break;
            case 2:
                pedal_label.setText("CHORUS");
                seqval_label.setText("MIX");
                manual_label.setVisibility(View.GONE);
                slModManual.setVisibility(View.GONE);
                break;
            case 3:
                pedal_label.setText("TREMOLO");
                seqval_label.setText("PITCH");
                manual_label.setVisibility(View.GONE);
                slModManual.setVisibility(View.GONE);
                break;
        }
    }
}
