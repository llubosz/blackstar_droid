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

public class DelayPedal {
    private AutoCompleteTextView delayTypeList;
    private Slider slDelayFeedback;
    private Slider slDelayLevel;
    private Slider slDelayTime;
    private MaterialButton delayPowerSwitch;
    private View delayPowerLed;
    public Control ctrlDelayFeedback;
    public Control ctrlDelayLevel;
    public Control ctrlDelayTime;
    public Control ctrlDelayType;
    public Control ctrlDelayPower;
    boolean delayPowerOn;

    BlackstarAmp amp;

    public void InitializeControls(final Context context,
                                   final BlackstarAmp amp,
                                   final View root,
                                   final Slider.OnChangeListener sliderChanged) {

        this.amp = amp;
        this.delayPowerOn = false;

        ctrlDelayPower = amp.Controls.get(16);
        ctrlDelayType = amp.Controls.get(23);
        ctrlDelayFeedback = amp.Controls.get(24);
        ctrlDelayLevel = amp.Controls.get(26);
        ctrlDelayTime = amp.Controls.get(27);

        delayTypeList = root.findViewById(R.id.delay_type_list);
        String[] delayTypes = context.getResources().getStringArray(R.array.delay_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, delayTypes);
        delayTypeList.setAdapter(adapter);
        delayTypeList.setOnItemClickListener((parent, view, position, id) ->
                amp.SetControlValue(ctrlDelayType, position));

        slDelayLevel = root.findViewById(R.id.delay_level_slider);
        slDelayLevel.addOnChangeListener(sliderChanged);
        slDelayFeedback = root.findViewById(R.id.delay_feedback_slider);
        slDelayFeedback.addOnChangeListener(sliderChanged);
        slDelayTime = root.findViewById(R.id.delay_time_slider);
        slDelayTime.addOnChangeListener(sliderChanged);

        delayPowerLed = root.findViewById(R.id.delay_power_led);
        delayPowerSwitch = root.findViewById(R.id.delay_power_switch);
        delayPowerSwitch.setOnClickListener(v -> {
            delayPowerOn = !delayPowerOn;
            ctrlDelayPower.controlValue = delayPowerOn ? 1 : 0;
            delayPowerLed.setBackground(ContextCompat.getDrawable(context,
                    delayPowerOn ? R.drawable.led_on : R.drawable.led_off));
            amp.SetControlValue(ctrlDelayPower, ctrlDelayPower.controlValue);
        });

        getInitialValues(context);
    }

    private void getInitialValues(Context context) {
        slDelayLevel.setValue(ctrlDelayLevel.controlValue);
        slDelayFeedback.setValue(ctrlDelayFeedback.controlValue);
        slDelayTime.setValue(ctrlDelayTime.controlValue);

        String[] delayTypes = context.getResources().getStringArray(R.array.delay_types);
        if (ctrlDelayType.controlValue >= 0 && ctrlDelayType.controlValue < delayTypes.length) {
            delayTypeList.setText(delayTypes[ctrlDelayType.controlValue], false);
        }

        delayPowerOn = ctrlDelayPower.controlValue == 1;
        delayPowerLed.setBackground(ContextCompat.getDrawable(context,
                delayPowerOn ? R.drawable.led_on : R.drawable.led_off));
    }
}
