package com.ca13b.blackdroid;

public class MockAmpState {

    private static final int MAX_PRESETS = 128;
    private static final int NAME_LENGTH = 21;
    private static final int SETTINGS_LENGTH = 42;

    private final byte[][] presetNames = new byte[MAX_PRESETS][NAME_LENGTH];
    private final byte[][] presetSettings = new byte[MAX_PRESETS][SETTINGS_LENGTH];
    private final byte[] currentControls = new byte[SETTINGS_LENGTH];
    private int currentPreset = 1;
    private boolean tunerMode = false;

    public MockAmpState() {
        initDefaults();
    }

    private void initDefaults() {
        // Initialize preset names: "Preset 1" through "Preset 128"
        for (int i = 0; i < MAX_PRESETS; i++) {
            String name = "Preset " + (i + 1);
            byte[] nameBytes = name.getBytes();
            System.arraycopy(nameBytes, 0, presetNames[i], 0, Math.min(nameBytes.length, NAME_LENGTH - 1));
        }

        // Default control values (index = control_id - 1 mapped into 42-byte array)
        // These go at offset control_id in the settings array (0-indexed from control_id 1)
        byte[] defaults = new byte[SETTINGS_LENGTH];
        defaults[0x01 - 1] = 0;       // voice = 0 (Clean Warm)
        defaults[0x02 - 1] = 64;      // gain = 64
        defaults[0x03 - 1] = 80;      // volume = 80
        defaults[0x04 - 1] = 64;      // bass = 64
        defaults[0x05 - 1] = 64;      // middle = 64
        defaults[0x06 - 1] = 64;      // treble = 64
        defaults[0x07 - 1] = 64;      // isf = 64
        defaults[0x08 - 1] = 0;       // tvp_value = 0
        defaults[0x0E - 1] = 0;       // tvp_switch = off
        defaults[0x0F - 1] = 0;       // mod_switch = off
        defaults[0x10 - 1] = 0;       // delay_switch = off
        defaults[0x11 - 1] = 0;       // reverb_switch = off
        defaults[0x12 - 1] = 0;       // mod_type = Flanger
        defaults[0x13 - 1] = 0;       // mod_segval = 0
        defaults[0x14 - 1] = 0;       // mod_manual = 0
        defaults[0x15 - 1] = 64;      // mod_level = 64
        defaults[0x16 - 1] = 64;      // mod_speed = 64
        defaults[0x17 - 1] = 0;       // delay_type = Linear
        defaults[0x18 - 1] = 0;       // delay_feedback = 0
        defaults[0x1A - 1] = 64;      // delay_level = 64
        defaults[0x1B - 1] = 100;     // delay_time_low (100ms = 0x64)
        defaults[0x1C - 1] = 0;       // delay_time_high
        defaults[0x1D - 1] = 0;       // reverb_type = Room
        defaults[0x1E - 1] = 0;       // reverb_size = 0
        defaults[0x20 - 1] = 64;      // reverb_level = 64
        defaults[0x24 - 1] = 1;       // fx_focus = Mod

        System.arraycopy(defaults, 0, currentControls, 0, SETTINGS_LENGTH);

        // Initialize all presets with default settings
        for (int i = 0; i < MAX_PRESETS; i++) {
            System.arraycopy(defaults, 0, presetSettings[i], 0, SETTINGS_LENGTH);
        }
    }

    public void loadPreset(int preset) {
        int idx = preset - 1;
        if (idx >= 0 && idx < MAX_PRESETS) {
            currentPreset = preset;
            System.arraycopy(presetSettings[idx], 0, currentControls, 0, SETTINGS_LENGTH);
        }
    }

    public void savePresetName(int preset, byte[] name) {
        int idx = preset - 1;
        if (idx >= 0 && idx < MAX_PRESETS) {
            presetNames[idx] = new byte[NAME_LENGTH];
            System.arraycopy(name, 0, presetNames[idx], 0, Math.min(name.length, NAME_LENGTH));
        }
    }

    public void savePresetSettings(int preset, byte[] settings) {
        int idx = preset - 1;
        if (idx >= 0 && idx < MAX_PRESETS) {
            System.arraycopy(settings, 0, presetSettings[idx], 0, Math.min(settings.length, SETTINGS_LENGTH));
        }
    }

    public byte[] getPresetName(int preset) {
        int idx = preset - 1;
        if (idx >= 0 && idx < MAX_PRESETS) {
            return presetNames[idx].clone();
        }
        return new byte[NAME_LENGTH];
    }

    public byte[] getPresetSettings(int preset) {
        int idx = preset - 1;
        if (idx >= 0 && idx < MAX_PRESETS) {
            return presetSettings[idx].clone();
        }
        return new byte[SETTINGS_LENGTH];
    }

    public void updateControl(int offset, byte value) {
        if (offset >= 0 && offset < SETTINGS_LENGTH) {
            currentControls[offset] = value;
        }
    }

    public int getCurrentPreset() {
        return currentPreset;
    }

    public boolean isTunerMode() {
        return tunerMode;
    }

    public void setTunerMode(boolean tunerMode) {
        this.tunerMode = tunerMode;
    }

    public byte[] buildAllControlsPacket() {
        byte[] packet = new byte[64];
        packet[0] = 0x03;
        packet[1] = 0x01; // voice control id
        packet[2] = (byte) currentPreset;
        packet[3] = 0x2A; // 42 = all-controls marker
        // Control values at offsets 4..45 (control_id + 3)
        for (int i = 0; i < SETTINGS_LENGTH && (i + 4) < 64; i++) {
            packet[i + 4] = currentControls[i];
        }
        return packet;
    }
}
