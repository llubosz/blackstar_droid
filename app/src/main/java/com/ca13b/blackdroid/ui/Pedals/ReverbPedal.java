package com.ca13b.blackdroid.ui.Pedals;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.core.content.ContextCompat;

import com.ca13b.blackdroid.BlackstarAmp;
import com.ca13b.blackdroid.Control;
import com.ca13b.blackdroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

public class ReverbPedal {

    private AutoCompleteTextView reverbTypeList;
    private Slider slReverbSize;
    private Slider slReverbLevel;
    private MaterialButton reverbPowerSwitch;
    private View reverbPowerLed;
    public Control ctrlReverbSize;
    public Control ctrlReverbLevel;
    public Control ctrlReverbType;
    public Control ctrlReverbPower;
    boolean reverbPowerOn;
    BlackstarAmp amp;

    public void InitializeControls(final Context context,
                                   final BlackstarAmp amp,
                                   final View root,
                                   final Slider.OnChangeListener sliderChanged) {

        this.amp = amp;

        ctrlReverbLevel = amp.Controls.get(32);
        ctrlReverbSize = amp.Controls.get(30);
        ctrlReverbType = amp.Controls.get(29);
        ctrlReverbPower = amp.Controls.get(17);

        reverbTypeList = root.findViewById(R.id.reverb_type_list);
        String[] reverbTypes = context.getResources().getStringArray(R.array.reverb_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, reverbTypes);
        reverbTypeList.setAdapter(adapter);
        reverbTypeList.setOnItemClickListener((parent, view, position, id) ->
                amp.SetControlValue(ctrlReverbType, position));

        slReverbLevel = root.findViewById(R.id.reverb_level_slider);
        slReverbLevel.addOnChangeListener(sliderChanged);
        slReverbSize = root.findViewById(R.id.reverb_size_slider);
        slReverbSize.addOnChangeListener(sliderChanged);

        reverbPowerLed = root.findViewById(R.id.reverb_power_led);
        reverbPowerSwitch = root.findViewById(R.id.reverb_power_switch);
        reverbPowerSwitch.setOnClickListener(v -> {
            reverbPowerOn = !reverbPowerOn;
            ctrlReverbPower.controlValue = reverbPowerOn ? 1 : 0;
            reverbPowerLed.setBackground(ContextCompat.getDrawable(context,
                    reverbPowerOn ? R.drawable.led_on : R.drawable.led_off));
            amp.SetControlValue(ctrlReverbPower, ctrlReverbPower.controlValue);
        });

        getInitialValues(context);
    }

    private void getInitialValues(Context context) {
        slReverbLevel.setValue(ctrlReverbLevel.controlValue);
        slReverbSize.setValue(ctrlReverbSize.controlValue);

        String[] reverbTypes = context.getResources().getStringArray(R.array.reverb_types);
        if (ctrlReverbType.controlValue >= 0 && ctrlReverbType.controlValue < reverbTypes.length) {
            reverbTypeList.setText(reverbTypes[ctrlReverbType.controlValue], false);
        }

        reverbPowerOn = ctrlReverbPower.controlValue == 1;
        reverbPowerLed.setBackground(ContextCompat.getDrawable(context,
                reverbPowerOn ? R.drawable.led_on : R.drawable.led_off));
    }
}
