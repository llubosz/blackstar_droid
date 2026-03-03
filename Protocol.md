# Blackstar ID Series USB Protocol

Reverse-engineered protocol for communicating with Blackstar ID:Core and ID:TVP
amplifiers over USB. Based on the [outsider](https://github.com/jonathanunderwood/outsider)
project by Jonathan Underwood and observations from the Blackdroid Android app.

## USB Device Identification

| Field      | Value  | Notes                        |
|------------|--------|------------------------------|
| Vendor ID  | 0x27D4 | Blackstar Amplification      |
| Product ID | 0x0001 | ID:TVP series                |
| Product ID | 0x0010 | ID:Core series               |
| Interface  | 0      | Single HID interface         |
| Endpoints  | 2      | One IN (interrupt), one OUT  |
| Packet size| 64     | All packets are 64 bytes     |

Communication uses USB interrupt transfers. All packets are exactly 64 bytes,
zero-padded. The host must detach any kernel HID driver before claiming the interface.

---

## Packet Format Overview

Every packet starts with a **type byte** at offset 0, followed by a **subtype byte**
at offset 1. The remaining bytes carry command-specific data.

```
Offset:  [0]    [1]      [2]      [3]       [4..63]
         type   subtype  varies   varies    payload / zero-padded
```

### Packet Types (byte 0)

| Type | Direction    | Purpose                        |
|------|-------------|--------------------------------|
| 0x02 | Both        | Preset operations              |
| 0x03 | Both        | Control value changes          |
| 0x07 | Amp -> Host | Startup response (firmware)    |
| 0x08 | Both        | Amp mode / state               |
| 0x09 | Amp -> Host | Tuner data                     |
| 0x81 | Host -> Amp | Startup / initialization       |

---

## 1. Startup Sequence (0x81)

### Request: Initialize Amp

Sent once at connection time. Triggers three response packets from the amp.

```
[0]  = 0x81
[1]  = 0x00
[2]  = 0x00
[3]  = 0x04
[4]  = 0x03
[5]  = 0x09  (observed: 0x06 in outsider, 0x09 in Blackdroid)
[6]  = 0x01  (observed: 0x02 in outsider, 0x01 in Blackdroid)
[7]  = 0xFF
[8..63] = 0x00
```

### Response Sequence (3 packets)

1. **Packet 1** (type 0x07) -- Firmware / device information
2. **Packet 2** (type 0x03, byte[3] = 0x2A) -- All current control settings
3. **Packet 3** (type 0x08) -- Current preset/mode state

---

## 2. Preset Operations (0x02)

All preset commands use type byte 0x02. The subtype (byte 1) determines the operation.
Preset numbers range from **1 to 128**.

### 2.1 Select Preset (Host -> Amp)

Switches the amp to the specified preset.

```
[0] = 0x02
[1] = 0x01  (select)
[2] = preset number (1-128)
[3] = 0x00
```

### 2.2 Write Preset Name (Host -> Amp)

Sets the name for a preset slot. **Must be sent before writing settings.**

```
[0]  = 0x02
[1]  = 0x02  (write name)
[2]  = preset number (1-128)
[3]  = 0x15  (21 = max name length)
[4..24] = ASCII name bytes (max 20 characters, null-terminated)
[25..63] = 0x00
```

### 2.3 Write Preset Settings (Host -> Amp)

Saves the current control values into a preset slot.

```
[0]  = 0x02
[1]  = 0x03  (write settings)
[2]  = preset number (1-128)
[3]  = 0x29  (41 = number of setting bytes)
[4..45] = control values (byte offset = control_id + 3)
[46..63] = 0x00
```

The settings payload mirrors the all-controls packet layout (see section 3.3).
Each control value is placed at offset `control_id + 3` in the packet.
Delay time occupies two bytes (see section 3 for encoding).

### 2.4 Request Preset Name (Host -> Amp)

Asks the amp to return the name of a preset. Response arrives asynchronously.

```
[0] = 0x02
[1] = 0x04  (request name)
[2] = preset number (1-128)
[3] = 0x00
```

### 2.5 Preset Name Response (Amp -> Host)

```
[0]  = 0x02
[1]  = 0x04
[2]  = preset number
[3]  = name length
[4..24] = ASCII name bytes (null-terminated)
```

### 2.6 Request Preset Settings (Host -> Amp)

```
[0] = 0x02
[1] = 0x05  (request settings)
[2] = preset number (1-128)
[3] = 0x00
```

### 2.7 Preset Settings Response (Amp -> Host)

```
[0]  = 0x02
[1]  = 0x05
[2]  = preset number
[3]  = 0x2A  (42 bytes of settings follow)
[4..45] = control values (same layout as all-controls packet)
```

### 2.8 Preset Changed Notification (Amp -> Host)

Sent when the user changes preset via the amp's front panel, or after a
select-preset command is acknowledged.

```
[0] = 0x02
[1] = 0x06  (preset changed)
[2] = new preset number
[3] = 0x00
```

---

## 3. Control Values (0x03)

### 3.1 Single-Value Control Change (Both directions)

Used for most controls (knobs, switches).

```
[0] = 0x03
[1] = control_id
[2] = 0x00
[3] = 0x01  (payload length: 1 byte)
[4] = value
```

### 3.2 Dual-Value Control Change (Both directions)

Used for delay time, and for effect type+segment pairs.

```
[0] = 0x03
[1] = control_id
[2] = 0x00
[3] = 0x02  (payload length: 2 bytes)
[4] = value_low / first value
[5] = value_high / second value
```

**Effect type+segment paired values:**
- Modulation: `[0x03, 0x12, 0x00, 0x02, mod_type, mod_segval]`
- Delay:      `[0x03, 0x17, 0x00, 0x02, delay_type, delay_feedback]`
- Reverb:     `[0x03, 0x1D, 0x00, 0x02, reverb_type, reverb_size]`

### 3.3 All-Controls Snapshot (Amp -> Host)

Sent as the second response to the startup packet. Contains every control
value in a single packet. Distinguished by byte[3] = 0x2A (42).

```
[0]  = 0x03
[1]  = control_id (of first/voice control)
[2]  = preset number currently loaded
[3]  = 0x2A  (42 -- distinguishes from single-value packet)
[4..45] = all control values
```

**Layout:** Each control value sits at offset `control_id + 3`:

| Offset | Control          | Range     |
|--------|------------------|-----------|
| 4      | voice            | 0x00-0x05 |
| 5      | gain             | 0x00-0x7F |
| 6      | volume           | 0x00-0x7F |
| 7      | bass             | 0x00-0x7F |
| 8      | middle           | 0x00-0x7F |
| 9      | treble           | 0x00-0x7F |
| 10     | isf              | 0x00-0x7F |
| 11     | tvp_valve        | 0x00-0x05 |
| 12     | mod_level        | 0x00-0x7F |
| 13     | mod_abspos       | 0x00-0x5F |
| 17     | tvp_switch       | 0x00-0x01 |
| 18     | mod_switch       | 0x00-0x01 |
| 19     | delay_switch     | 0x00-0x01 |
| 20     | reverb_switch    | 0x00-0x01 |
| 21     | mod_type         | 0x00-0x03 |
| 22     | mod_segval       | 0x00-0x1F |
| 23     | mod_manual       | 0x00-0x7F |
| 25     | mod_speed        | 0x00-0x7F |
| 26     | delay_type       | 0x00-0x03 |
| 27     | delay_feedback   | 0x00-0x1F |
| 29     | delay_level      | 0x00-0x7F |
| 30     | delay_time_low   | 0x00-0xFF |
| 31     | delay_time_high  | 0x00-0x07 |
| 32     | reverb_type      | 0x00-0x03 |
| 33     | reverb_size      | 0x00-0x1F |
| 35     | reverb_level     | 0x00-0x7F |
| 39     | fx_focus         | 0x01-0x03 |

---

## 4. Control Reference

### 4.1 Amp Controls

| Control         | ID   | Min  | Max  | Notes                                    |
|-----------------|------|------|------|------------------------------------------|
| voice           | 0x01 | 0    | 5    | Clean Warm, Clean Bright, Crunch, Super Crunch, OD1, OD2 |
| gain            | 0x02 | 0    | 127  |                                          |
| volume          | 0x03 | 0    | 127  |                                          |
| bass            | 0x04 | 0    | 127  |                                          |
| middle          | 0x05 | 0    | 127  |                                          |
| treble          | 0x06 | 0    | 127  |                                          |
| isf             | 0x07 | 0    | 127  | Infinite Shape Feature                   |
| tvp_valve       | 0x08 | 0    | 5    | EL84, 6V6, EL34, KT66, 6L6, KT88       |
| mod_abspos      | 0x0A | 0    | 127  | Absolute knob position (read-only)       |
| resonance       | 0x0B | 0    | 127  |                                          |
| presence        | 0x0C | 0    | 127  |                                          |
| master_volume   | 0x0D | 0    | 127  |                                          |

### 4.2 Switch Controls

| Control         | ID   | Values | Notes                                   |
|-----------------|------|--------|-----------------------------------------|
| tvp_switch      | 0x0E | 0 / 1  | TVP emulation on/off                   |
| mod_switch      | 0x0F | 0 / 1  | Modulation effect on/off               |
| delay_switch    | 0x10 | 0 / 1  | Delay effect on/off                    |
| reverb_switch   | 0x11 | 0 / 1  | Reverb effect on/off                   |

### 4.3 Modulation Controls

| Control         | ID   | Min  | Max  | Notes                                    |
|-----------------|------|------|------|------------------------------------------|
| mod_type        | 0x12 | 0    | 3    | Flanger, Phaser, Tremolo, Chorus         |
| mod_segval      | 0x13 | 0    | 31   | Segmented selector value                 |
| mod_manual      | 0x14 | 0    | 127  | Flanger only; exposed via Insider software |
| mod_level       | 0x15 | 0    | 127  |                                          |
| mod_speed       | 0x16 | 0    | 127  |                                          |

### 4.4 Delay Controls

| Control           | ID   | Min  | Max  | Notes                                  |
|-------------------|------|------|------|----------------------------------------|
| delay_type        | 0x17 | 0    | 3    | Linear, Analog, Tape, Multi            |
| delay_feedback    | 0x18 | 0    | 31   | Segmented selector value               |
| delay_level       | 0x1A | 0    | 127  |                                        |
| delay_time        | 0x1B | 100  | 2000 | Milliseconds (16-bit, see encoding)    |
| delay_time_coarse | 0x1C | 0    | 7    | High byte of delay time                |

**Delay time encoding:**
```
delay_ms = (delay_time_coarse * 256) + delay_time
```
Examples: 100ms = [0x64, 0x00], 1012ms = [0xF4, 0x03], 2000ms = [0xD0, 0x07]

### 4.5 Reverb Controls

| Control         | ID   | Min  | Max  | Notes                                    |
|-----------------|------|------|------|------------------------------------------|
| reverb_type     | 0x1D | 0    | 3    | Room, Hall, Spring, Plate               |
| reverb_size     | 0x1E | 0    | 31   | Segmented selector value                 |
| reverb_level    | 0x20 | 0    | 127  |                                          |

**Known firmware bug:** Adjusting reverb_level also changes the byte at offset 12
(mod_level position) in the all-controls packet. Adjusting mod_level only changes
offset 12. The true reverb_level is at offset 35.

### 4.6 Effect Focus

| Control   | ID   | Values | Notes                                        |
|-----------|------|--------|----------------------------------------------|
| fx_focus  | 0x24 | 1-3    | 1 = Mod, 2 = Delay, 3 = Reverb              |

Indicates which effect currently has "focus" -- i.e. which effect's LED is lit
green on the front panel, and which is being controlled by the shared Level /
Type / Tap controls.

---

## 5. Amp Mode (0x08)

### 5.1 Manual Mode Notification (Amp -> Host)

```
[0] = 0x08
[1] = 0x03
[2] = 0x00
[3] = 0x01
[4] = mode  (0x01 = manual mode, 0x00 = preset mode)
```

Sent when the amp switches between manual mode (user is tweaking controls
directly) and preset mode (a stored preset is loaded).

### 5.2 Tuner Mode (Both directions)

**Enable/disable tuner (Host -> Amp):**
```
[0] = 0x08
[1] = 0x11
[2] = 0x00
[3] = 0x01
[4] = state  (0x01 = enable, 0x00 = disable)
```

**Tuner state notification (Amp -> Host):**
Same format -- sent when tuner mode is activated via the amp's front panel.

---

## 6. Tuner Data (0x09)

Sent continuously while the amp is in tuner mode.

```
[0] = 0x09
[1] = note_id   (0x00 = no note detected)
[2] = pitch      (0-99 decimal, 50 = in tune)
```

### Note Mapping

| Byte | Note |
|------|------|
| 0x01 | E    |
| 0x02 | F    |
| 0x03 | F#   |
| 0x04 | G    |
| 0x05 | Ab   |
| 0x06 | A    |
| 0x07 | Bb   |
| 0x08 | B    |
| 0x09 | C    |
| 0x0A | C#   |
| 0x0B | D    |
| 0x0C | Eb   |

### Pitch Interpretation

- Value 50 (0x32) = perfectly in tune
- Values < 50 = flat (lower = more flat)
- Values > 50 = sharp (higher = more sharp)
- Delta from standard tuning: `pitch - 50`

**Standard tuning reference (all should read 0x32):**
E=0x32, A=0x32, D=0x32, G=0x32, B=0x32

---

## 7. Typical Communication Flows

### Startup
```
Host -> Amp:  [0x81 0x00 0x00 0x04 0x03 0x09 0x01 0xFF ...]
Amp -> Host:  [0x07 ...] (firmware info)
Amp -> Host:  [0x03 ... 0x2A ...] (all controls)
Amp -> Host:  [0x08 ...] (mode state)
```

### Change a Control
```
Host -> Amp:  [0x03 0x02 0x00 0x01 0x50]  (set gain to 80)
Amp -> Host:  [0x03 0x02 0x00 0x01 0x50]  (confirmation echo)
```

### Load a Preset
```
Host -> Amp:  [0x02 0x01 0x05 0x00]       (select preset 5)
Amp -> Host:  [0x02 0x06 0x05 0x00]       (preset changed to 5)
Amp -> Host:  [0x03 ... 0x2A ...]         (new control values)
```

### Save Current Settings to Preset
```
Host -> Amp:  [0x02 0x02 0x05 0x15 name...]  (write name to slot 5)
Host -> Amp:  [0x02 0x03 0x05 0x29 data...]  (write settings to slot 5)
```

### Read All Preset Names
```
for preset 1..128:
    Host -> Amp:  [0x02 0x04 preset 0x00]
    Amp -> Host:  [0x02 0x04 preset ... name...]
```

### Tuner Session
```
Host -> Amp:  [0x08 0x11 0x00 0x01 0x01]  (enable tuner)
Amp -> Host:  [0x09 0x06 0x32 ...]        (A, in tune)
Amp -> Host:  [0x09 0x01 0x28 ...]        (E, flat)
...
Host -> Amp:  [0x08 0x11 0x00 0x01 0x00]  (disable tuner)
```

---

## References

- [outsider](https://github.com/jonathanunderwood/outsider) -- Python library
  for Blackstar ID amps by Jonathan Underwood (primary protocol source)
- [Blackdroid](https://github.com/) -- Android controller app (this project)
- Blackstar Insider -- Official (closed-source) desktop software
