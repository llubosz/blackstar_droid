package com.ca13b.blackdroid.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.ca13b.blackdroid.BlackstarAmp;
import com.ca13b.blackdroid.Control;
import com.ca13b.blackdroid.MainActivity;
import com.ca13b.blackdroid.R;
import com.ca13b.blackdroid.ui.Pedals.DelayPedal;
import com.ca13b.blackdroid.ui.Pedals.ModPedal;
import com.ca13b.blackdroid.ui.Pedals.ReverbPedal;
import com.google.android.material.slider.Slider;

public class EffectsFragment extends Fragment {

    private EffectsViewModel effectsViewModel;
    private BlackstarAmp amp;
    private ReverbPedal reverbPedal = new ReverbPedal();
    private DelayPedal delayPedal = new DelayPedal();
    private ModPedal modPedal = new ModPedal();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        amp = MainActivity.blackstarAmp;

        effectsViewModel =
                ViewModelProviders.of(this).get(EffectsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_effects, container, false);

        reverbPedal.InitializeControls(getContext(), amp, root.findViewById(R.id.reverb_pedal), sliderChanged);
        delayPedal.InitializeControls(getContext(), amp, root.findViewById(R.id.delay_pedal), sliderChanged);
        modPedal.InitializeControls(getContext(), amp, root.findViewById(R.id.mod_pedal), sliderChanged);
        return root;
    }

    public Slider.OnChangeListener sliderChanged = (slider, value, fromUser) -> {
        if (!fromUser) return;
        int progress = (int) value;
        Control ctrlTemp = null;

        int id = slider.getId();
        if (id == R.id.reverb_size_slider) {
            ctrlTemp = reverbPedal.ctrlReverbSize;
        } else if (id == R.id.reverb_level_slider) {
            ctrlTemp = reverbPedal.ctrlReverbLevel;
        } else if (id == R.id.delay_feedback_slider) {
            ctrlTemp = delayPedal.ctrlDelayFeedback;
        } else if (id == R.id.delay_level_slider) {
            ctrlTemp = delayPedal.ctrlDelayLevel;
        } else if (id == R.id.delay_time_slider) {
            ctrlTemp = delayPedal.ctrlDelayTime;
        } else if (id == R.id.mod_depth_slider) {
            ctrlTemp = modPedal.ctrlModDepth;
        } else if (id == R.id.mod_seqval_slider) {
            ctrlTemp = modPedal.ctrlModSeqVal;
        } else if (id == R.id.mod_manual_slider) {
            ctrlTemp = modPedal.ctrlModManual;
        } else if (id == R.id.mod_speed_slider) {
            ctrlTemp = modPedal.ctrlModSpeed;
        }

        if (ctrlTemp == null) return;
        ctrlTemp.controlValue = progress;
        amp.SetControlValue(ctrlTemp, (progress * ctrlTemp.maxValue) / 100);
    };

    @Override
    public void onDestroy() {
        amp = null;
        reverbPedal = null;
        modPedal = null;
        delayPedal = null;
        super.onDestroy();
    }
}
