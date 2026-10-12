package it.cnr.speech.energypitch.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import it.cnr.speech.segmentation.Energy;
import it.cnr.speech.segmentation.Pitch;
import it.cnr.speech.utils.Utils;

public class EnergyPitchExtractor {

	
    public void extractEnergyPitch(File waveFile, File outputFile,
            double windowLength, double windowHop, double minPitchFreq,
            double maxPitchFreq, boolean normalizeEnergy) throws Exception {
        extractEnergyPitch(waveFile, outputFile, windowLength, windowHop,
                minPitchFreq, maxPitchFreq, normalizeEnergy, false, false);
    }

    public void extractEnergyPitch(File waveFile, File outputFile,
            double windowLength, double windowHop, double minPitchFreq,
            double maxPitchFreq, boolean normalizeEnergy,
            boolean useDelta, boolean useDoubleDelta) throws Exception {
        double[] energy = new Energy().energyCurve(waveFile.getAbsolutePath(),
                windowLength, windowHop, normalizeEnergy);
        double[] pitch = new Pitch().calculatePitch(waveFile.getAbsolutePath(),
                windowLength, windowHop, minPitchFreq, maxPitchFreq);
        if (energy.length != pitch.length) {
            throw new IllegalStateException("Energy and pitch contours have different lengths");
        }

        double[] energyDelta = null;
        double[] pitchDelta = null;
        double[] energyDoubleDelta = null;
        double[] pitchDoubleDelta = null;
        if (useDelta || useDoubleDelta) {
            energyDelta = Utils.calculateDelta(energy);
            pitchDelta = Utils.calculateDelta(pitch);
        }
        if (useDoubleDelta) {
            energyDoubleDelta = Utils.calculateDelta(energyDelta);
            pitchDoubleDelta = Utils.calculateDelta(pitchDelta);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            bw.write("Time_start_s,Time_end_s,energy,pitch");
            if (useDelta) bw.write(",energy_delta,pitch_delta");
            if (useDoubleDelta) bw.write(",energy_double_delta,pitch_double_delta");
            bw.newLine();
            for (int i = 0; i < energy.length; i++) {
                double t0 = i * windowHop;
                bw.write(t0 + "," + (t0 + windowLength) + "," + energy[i] + "," + pitch[i]);
                if (useDelta) bw.write("," + energyDelta[i] + "," + pitchDelta[i]);
                if (useDoubleDelta) bw.write("," + energyDoubleDelta[i] + "," + pitchDoubleDelta[i]);
                bw.newLine();
            }
        }
    }

    

	public static void main(String[] args) {

	    String usage = """
	            Usage:
	              java it.cnr.speech.energypitch.main.EnergyPitchExtractor \\
	                -input_file "/full/path/input.wav" \\
	                -output_file "/full/path/output.csv" \\
	                [-window_length 0.1] \\
	                [-window_hop 0.05] \\
	                [-min_pitch_freq 60] \\
	                [-max_pitch_freq 250] \\
	                [-normalize_energy false]
	                [-use_delta false]
	                [-use_double_delta false]

	            Preferred input: 16 kHz, signed 16-bit, little-endian, mono PCM WAV.
	            Window length and hop are in seconds.
	            Defaults: window length = 0.1 s (100 ms), hop = 0.05 s (50 ms).
	            """;

	    if (args.length == 0) {
	        System.err.println(usage);
	        System.exit(1);
	        return;
	    }

	    File inputFile = null;
	    File outputFile = null;

	    double windowLength = 0.100;
	    double windowHop = 0.050;
	    double minPitchFreq = 60.0;
	    double maxPitchFreq = 250.0;
	    boolean normalizeEnergy = false;
        boolean useDelta = false;
        boolean useDoubleDelta = false;

	    try {
	        java.util.Set<String> seen = new java.util.HashSet<>();

	        for (int i = 0; i < args.length; i++) {
	            String option = args[i];

	            if ("-help".equals(option) || "--help".equals(option)) {
	                System.out.println(usage);
	                return;
	            }

	            switch (option) {
	                case "-input_file":
	                case "-output_file":
	                case "-window_length":
	                case "-window_hop":
	                case "-min_pitch_freq":
	                case "-max_pitch_freq":
	                case "-normalize_energy":
                case "-use_delta":
                case "-use_double_delta":
	                    break;
	                default:
	                    throw new IllegalArgumentException(
	                            "Unknown option: " + option);
	            }

	            if (!seen.add(option)) {
	                throw new IllegalArgumentException(
	                        "Duplicate option: " + option);
	            }

	            if (i + 1 >= args.length || args[i + 1].startsWith("-")) {
	                throw new IllegalArgumentException(
	                        "Missing or invalid value for " + option);
	            }

	            String value = args[++i];

	            switch (option) {
	                case "-input_file":
	                    inputFile = new File(value);
	                    break;

	                case "-output_file":
	                    outputFile = new File(value);
	                    break;

	                case "-window_length":
	                    windowLength = Double.parseDouble(value);
	                    break;

	                case "-window_hop":
	                    windowHop = Double.parseDouble(value);
	                    break;

	                case "-min_pitch_freq":
	                    minPitchFreq = Double.parseDouble(value);
	                    break;

	                case "-max_pitch_freq":
	                    maxPitchFreq = Double.parseDouble(value);
	                    break;

	                case "-normalize_energy":
                case "-use_delta":
                case "-use_double_delta":
	                    if (!"true".equalsIgnoreCase(value)
	                            && !"false".equalsIgnoreCase(value)) {
	                        throw new IllegalArgumentException(
	                                option + " must be true or false.");
	                    }
	                    switch (option) {
                        case "-normalize_energy": normalizeEnergy = Boolean.parseBoolean(value); break;
                        case "-use_delta": useDelta = Boolean.parseBoolean(value); break;
                        case "-use_double_delta": useDoubleDelta = Boolean.parseBoolean(value); break;
                    }
	                    break;
	            }
	        }

	        if (inputFile == null || outputFile == null) {
	            throw new IllegalArgumentException(
	                    "-input_file and -output_file are required.");
	        }

	        if (!inputFile.isFile() || !inputFile.canRead()) {
	            throw new IllegalArgumentException(
	                    "Input file does not exist or is not readable: " + inputFile);
	        }

	        if (!Double.isFinite(windowLength) || windowLength <= 0
	                || !Double.isFinite(windowHop) || windowHop <= 0) {
	            throw new IllegalArgumentException(
	                    "Window length and hop must be finite and greater than zero.");
	        }

	        if (!Double.isFinite(minPitchFreq)
	                || !Double.isFinite(maxPitchFreq)
	                || minPitchFreq <= 0
	                || maxPitchFreq <= minPitchFreq) {
	            throw new IllegalArgumentException(
	                    "Pitch frequencies must be finite and satisfy "
	                            + "0 < min_pitch_freq < max_pitch_freq.");
	        }

	        if (inputFile.getCanonicalFile().equals(outputFile.getCanonicalFile())
	                || (outputFile.exists()
	                    && java.nio.file.Files.isSameFile(
	                            inputFile.toPath(), outputFile.toPath()))) {
	            throw new IllegalArgumentException(
	                    "Input and output must be different files.");
	        }

	    } catch (Exception e) {
	        System.err.println("Invalid arguments: " + e.getMessage());
	        System.err.println(usage);
	        System.exit(1);
	        return;
	    }

	    try {
	        EnergyPitchExtractor extractor = new EnergyPitchExtractor();

	        extractor.extractEnergyPitch(
	                inputFile,
	                outputFile,
	                windowLength,
	                windowHop,
	                minPitchFreq,
	                maxPitchFreq,
	                normalizeEnergy, useDelta, useDoubleDelta);

	        System.out.println(
	                "Energy and pitch saved to: " + outputFile.getAbsolutePath());

	    } catch (Exception e) {
	        System.err.println(
	                "Energy/pitch extraction failed: " + e.getMessage());
	        e.printStackTrace(System.err);
	        System.exit(2);
	    }
	}
	
}
