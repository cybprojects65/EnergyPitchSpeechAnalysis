# Energy and Pitch Speech Analysis

Java software for extracting energy and pitch contours from WAV audio and exporting them to CSV, using configurable window length and hop. Pitch extraction uses AuToBI. Optional delta and double-delta features describe changes in both contours across frames.

## Requirements and input format

Use Java 21 to run the application. After replacing `EnergyPitchExtractor.java` with the version supporting delta features, rebuild `energypitch.jar` before using the new options.

The preferred input audio format is **16 kHz, signed 16-bit, little-endian, mono PCM WAV**.

## Usage

Run the following command from the project directory:

```bash
java -jar energypitch.jar -input_file "./samples/PS4Audio_cut_1min.wav" -output_file "./samples/PS4Audio_cut_1min_ep.csv" -window_length 0.1 -window_hop 0.05 -min_pitch_freq 60 -max_pitch_freq 250 -normalize_energy false
```

Window length and hop are in **seconds**. Defaults are **0.1 s (100 ms)** for window length and **0.05 s (50 ms)** for hop. The default pitch search range is **60–250 Hz**.

To include both delta and double-delta features:

```bash
java -jar energypitch.jar -input_file "./samples/PS4Audio_cut_1min.wav" -output_file "./samples/PS4Audio_cut_1min_ep.csv" -window_length 0.1 -window_hop 0.05 -min_pitch_freq 60 -max_pitch_freq 250 -normalize_energy false -use_delta true -use_double_delta true
```

## Command-line options

| Option | Description | Default |
| --- | --- | --- |
| `-input_file` | Path to the input WAV file; relative and absolute paths are accepted. | Required |
| `-output_file` | Path to the output CSV file. | Required |
| `-window_length` | Output window length in seconds. | `0.1` |
| `-window_hop` | Time between successive window starts, in seconds. | `0.05` |
| `-min_pitch_freq` | Minimum pitch frequency in Hz. | `60` |
| `-max_pitch_freq` | Maximum pitch frequency in Hz. | `250` |
| `-normalize_energy` | Normalize energy by the maximum window energy in the recording (`true` or `false`). | `false` |
| `-use_delta` | Add delta columns for both energy and pitch (`true` or `false`). | `false` |
| `-use_double_delta` | Add double-delta columns for both energy and pitch (`true` or `false`). | `false` |
| `-help` or `--help` | Display usage information. | — |

Window length and hop must be finite and positive. Pitch limits must be finite and satisfy `0 < min_pitch_freq < max_pitch_freq`. Input and output must be different files. The output directory must already exist; an existing output file is overwritten.

To use all defaults:

```bash
java -jar energypitch.jar -input_file "./samples/PS4Audio_cut_1min.wav" -output_file "./samples/PS4Audio_cut_1min_ep.csv"
```

## CSV output

The output contains one row per complete window, with the following default header:

```csv
Time_start_s,Time_end_s,energy,pitch
```

| Column | Description |
| --- | --- |
| `Time_start_s` | Window start time in seconds. |
| `Time_end_s` | Window end time in seconds. |
| `energy` | Mean-square sample amplitude, optionally normalized. |
| `pitch` | Pitch value in Hz. |

With the default settings, successive windows cover 0–0.1 s, 0.05–0.15 s, 0.1–0.2 s, and so on. Incomplete trailing windows are discarded.

Energy normalization is disabled by default. When enabled, the maximum nonzero window energy becomes `1`; silence remains `0`.

## Delta and double-delta features

Both options default to `false` and can be enabled independently. With both enabled, the CSV header is:

```csv
Time_start_s,Time_end_s,energy,pitch,energy_delta,pitch_delta,energy_double_delta,pitch_double_delta
```

| Option | Columns appended |
| --- | --- |
| `-use_delta true` | `energy_delta,pitch_delta` |
| `-use_double_delta true` | `energy_double_delta,pitch_double_delta` |

Only requested columns are exported. When double deltas are enabled without deltas, deltas are still computed internally. All features retain the same frame count and timestamps as the base contours.

For either contour `x`, deltas use a five-frame regression:

```text
delta[t] = ((x[t+1] - x[t-1]) + 2 * (x[t+2] - x[t-2])) / 10
```

Double deltas apply the same calculation to the delta contour. Indices outside the contour repeat the nearest endpoint value. Empty contours remain empty, and a single-frame contour has zero delta and double delta.

These features measure change **per frame**, not per second; they are not divided by the window hop. Changing the hop therefore changes their temporal interpretation. Energy deltas use the energy contour after any requested normalization. Pitch values of `0` participate in the calculation, so voiced/unvoiced transitions can produce nonzero pitch deltas.

## Code structure

- `src/it/cnr/speech/energypitch/main/EnergyPitchExtractor.java`: command-line entry point, energy/pitch extraction, delta and double-delta calculation, and CSV export. The original seven-argument `extractEnergyPitch` method remains supported and disables both optional feature types; a nine-argument overload adds `useDelta` and `useDoubleDelta`.
- `src/it/cnr/speech/segmentation/Energy.java`: windowed mean-square energy calculation and optional normalization.
- `src/it/cnr/speech/segmentation/Pitch.java`: AuToBI-based pitch extraction and window aggregation.
- `src/it/cnr/speech/audiofeatures/`: audio loading and supporting audio-processing classes.
- `src/it/cnr/speech/utils/`: supporting utilities.
- `lib/AuToBI.jar`: bundled AuToBI dependency.
- `energypitch.jar`: executable application.
- `samples/`: example WAV files and CSV outputs.

The main class is `it.cnr.speech.energypitch.main.EnergyPitchExtractor`.

## Error handling

Invalid command-line arguments produce an error message and exit code `1`. Extraction failures produce an error message and stack trace, with exit code `2`.
