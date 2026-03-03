package com.ca13b.blackdroid;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MockAmpCommunicator implements AmpCommunicator {

    private static final String tag = "BSD/MockAmp";
    private static final long RESPONSE_DELAY_MS = 5;
    private static final long TUNER_INTERVAL_MS = 200;

    private final Context context;
    private final BlackstarAmp amp;
    private final MockAmpState state;
    private final HandlerThread handlerThread;
    private final Handler mockHandler;
    private final Random random = new Random();

    private ScheduledExecutorService tunerExecutor;
    private ScheduledFuture<?> tunerFuture;

    public MockAmpCommunicator(Context context, BlackstarAmp amp) {
        this.context = context;
        this.amp = amp;
        this.state = new MockAmpState();
        this.handlerThread = new HandlerThread("MockAmpThread");
        this.handlerThread.start();
        this.mockHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void setUpDevice() {
        Log.i(tag, "Mock amp device set up");
    }

    @Override
    public void SendData(byte[] data) {
        if (data == null || data.length < 1) return;

        Log.i(tag, "Mock received packet type: 0x" + String.format("%02X", data[0] & 0xFF));

        int type = data[0] & 0xFF;
        switch (type) {
            case 0x81:
                handleStartup();
                break;
            case 0x02:
                handlePresetCommand(data);
                break;
            case 0x03:
                handleControlChange(data);
                break;
            case 0x08:
                handleModeCommand(data);
                break;
            default:
                Log.i(tag, "Mock ignoring unknown packet type: 0x" + String.format("%02X", type));
                break;
        }
    }

    @Override
    public void shutdown() {
        stopTuner();
        handlerThread.quitSafely();
        Log.i(tag, "Mock amp shut down");
    }

    private void handleStartup() {
        Log.i(tag, "Mock handling startup");

        mockHandler.postDelayed(() -> {
            // Response 1: Firmware info (type 0x07)
            byte[] firmware = new byte[64];
            firmware[0] = 0x07;
            firmware[1] = 0x01;
            firmware[4] = 0x01; // firmware version placeholder
            firmware[5] = 0x00;
            firmware[6] = 0x10; // product id: id-core
            Log.i(tag, "Mock sending firmware response");
            // Firmware packet is logged but not acted upon by the app

            // Response 2: All controls (type 0x03, byte[3] = 0x2A)
            byte[] allControls = state.buildAllControlsPacket();
            Log.i(tag, "Mock sending all-controls response");
            amp.SetControlsFromPacket(ByteBuffer.wrap(allControls));

            // Response 3: Mode state (type 0x08)
            byte[] modePacket = new byte[64];
            modePacket[0] = 0x08;
            modePacket[1] = 0x03;
            modePacket[3] = 0x01;
            modePacket[4] = 0x00; // preset mode
            Log.i(tag, "Mock sending mode response");
            // Mode packet logged but HandleAmpMode just logs it

            // Refresh UI on main thread
            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (activity.getFragmentRefreshListener() != null) {
                        activity.getFragmentRefreshListener().onRefresh(ByteBuffer.wrap(allControls));
                    }
                });
            }
        }, RESPONSE_DELAY_MS);
    }

    private void handlePresetCommand(byte[] data) {
        if (data.length < 2) return;

        int subtype = data[1] & 0xFF;
        int presetNumber = data.length > 2 ? (data[2] & 0xFF) : 1;

        switch (subtype) {
            case 0x01: // Select preset
                handleSelectPreset(presetNumber);
                break;
            case 0x02: // Write preset name
                handleWritePresetName(presetNumber, data);
                break;
            case 0x03: // Write preset settings
                handleWritePresetSettings(presetNumber, data);
                break;
            case 0x04: // Request preset name
                handleRequestPresetName(presetNumber);
                break;
            case 0x05: // Request preset settings
                handleRequestPresetSettings(presetNumber);
                break;
            default:
                Log.i(tag, "Mock ignoring preset subtype: 0x" + String.format("%02X", subtype));
                break;
        }
    }

    private void handleSelectPreset(int presetNumber) {
        Log.i(tag, "Mock selecting preset " + presetNumber);
        state.loadPreset(presetNumber);

        mockHandler.postDelayed(() -> {
            // Send preset-changed notification (0x02/0x06)
            byte[] changedPacket = new byte[64];
            changedPacket[0] = 0x02;
            changedPacket[1] = 0x06;
            changedPacket[2] = (byte) presetNumber;

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.HandlePresetData(6, presetNumber, ByteBuffer.wrap(changedPacket));
            }

            // Send all-controls with new preset values
            byte[] allControls = state.buildAllControlsPacket();
            amp.SetControlsFromPacket(ByteBuffer.wrap(allControls));

            // Refresh UI
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (activity.getFragmentRefreshListener() != null) {
                        activity.getFragmentRefreshListener().onRefresh(ByteBuffer.wrap(allControls));
                    }
                });
            }
        }, RESPONSE_DELAY_MS);
    }

    private void handleWritePresetName(int presetNumber, byte[] data) {
        Log.i(tag, "Mock writing preset name for preset " + presetNumber);
        byte[] name = new byte[21];
        int len = Math.min(data.length - 4, 21);
        if (len > 0) {
            System.arraycopy(data, 4, name, 0, len);
        }
        state.savePresetName(presetNumber, name);
    }

    private void handleWritePresetSettings(int presetNumber, byte[] data) {
        Log.i(tag, "Mock writing preset settings for preset " + presetNumber);
        byte[] settings = new byte[42];
        int len = Math.min(data.length - 4, 42);
        if (len > 0) {
            System.arraycopy(data, 4, settings, 0, len);
        }
        state.savePresetSettings(presetNumber, settings);
    }

    private void handleRequestPresetName(int presetNumber) {
        Log.i(tag, "Mock returning preset name for preset " + presetNumber);

        mockHandler.postDelayed(() -> {
            byte[] nameBytes = state.getPresetName(presetNumber);
            byte[] response = new byte[64];
            response[0] = 0x02;
            response[1] = 0x04;
            response[2] = (byte) presetNumber;
            // Calculate name length
            int nameLen = 0;
            for (int i = 0; i < nameBytes.length; i++) {
                if (nameBytes[i] == 0) break;
                nameLen++;
            }
            response[3] = (byte) nameLen;
            System.arraycopy(nameBytes, 0, response, 4, Math.min(nameBytes.length, 21));

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.HandlePresetData(4, presetNumber, ByteBuffer.wrap(response));
            }
        }, RESPONSE_DELAY_MS);
    }

    private void handleRequestPresetSettings(int presetNumber) {
        Log.i(tag, "Mock returning preset settings for preset " + presetNumber);

        mockHandler.postDelayed(() -> {
            byte[] settings = state.getPresetSettings(presetNumber);
            byte[] response = new byte[64];
            response[0] = 0x02;
            response[1] = 0x05;
            response[2] = (byte) presetNumber;
            response[3] = 0x2A; // 42 bytes of settings
            System.arraycopy(settings, 0, response, 4, Math.min(settings.length, 42));

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.HandlePresetData(5, presetNumber, ByteBuffer.wrap(response));
            }
        }, RESPONSE_DELAY_MS);
    }

    private void handleControlChange(byte[] data) {
        if (data.length < 5) return;

        int controlId = data[1] & 0xFF;
        byte value = data[4];

        Log.i(tag, "Mock control change: id=0x" + String.format("%02X", controlId) + " value=" + (value & 0xFF));

        // Update state (controlId maps to offset controlId-1 in the settings array)
        if (controlId > 0) {
            state.updateControl(controlId - 1, value);
            // Handle dual-byte delay time
            if (data[3] == 0x02 && data.length > 5) {
                state.updateControl(controlId, data[5]);
            }
        }

        // Echo back the same packet
        mockHandler.postDelayed(() -> {
            BlackstarAmp.HandleControlValueResponse(data[1], value);
        }, RESPONSE_DELAY_MS);
    }

    private void handleModeCommand(byte[] data) {
        if (data.length < 5) return;

        int subtype = data[1] & 0xFF;
        if (subtype == 0x11) {
            boolean enable = data[4] == 0x01;
            Log.i(tag, "Mock tuner mode: " + (enable ? "ON" : "OFF"));
            state.setTunerMode(enable);

            // Echo the tuner mode packet
            mockHandler.postDelayed(() -> {
                byte[] response = new byte[64];
                response[0] = 0x08;
                response[1] = 0x11;
                response[3] = 0x01;
                response[4] = enable ? (byte) 0x01 : (byte) 0x00;

                MainActivity activity = MainActivity.getInstance();
                if (activity != null) {
                    activity.SetTunerUI(ByteBuffer.wrap(response));
                }
            }, RESPONSE_DELAY_MS);

            if (enable) {
                startTuner();
            } else {
                stopTuner();
            }
        }
    }

    private void startTuner() {
        stopTuner();
        tunerExecutor = Executors.newSingleThreadScheduledExecutor();
        tunerFuture = tunerExecutor.scheduleAtFixedRate(() -> {
            if (!state.isTunerMode()) {
                stopTuner();
                return;
            }

            byte[] tunerPacket = new byte[64];
            tunerPacket[0] = 0x09;
            tunerPacket[1] = (byte) (1 + random.nextInt(12)); // note 0x01-0x0C
            tunerPacket[2] = (byte) (30 + random.nextInt(41)); // pitch 30-70, centered around 50

            MainActivity activity = MainActivity.getInstance();
            if (activity != null) {
                activity.SetTunerUI(ByteBuffer.wrap(tunerPacket));
            }
        }, TUNER_INTERVAL_MS, TUNER_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopTuner() {
        if (tunerFuture != null) {
            tunerFuture.cancel(false);
            tunerFuture = null;
        }
        if (tunerExecutor != null) {
            tunerExecutor.shutdownNow();
            tunerExecutor = null;
        }
    }
}
