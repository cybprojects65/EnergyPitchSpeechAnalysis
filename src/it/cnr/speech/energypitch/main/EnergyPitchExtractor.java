package it.cnr.speech.energypitch.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import it.cnr.speech.segmentation.Energy;
import it.cnr.speech.segmentation.Pitch;

public class EnergyPitchExtractor {

	
	public void extractEnergyPitch(File waveFile, File outputFile, double windowLength, double windowHop, double minPitchFreq, double maxPitchFreq, boolean normalizeEnergy) throws Exception{
		
		
		Energy nrg = new Energy();
		Pitch pitch = new Pitch();
        
		System.out.println("Extracting energy on "+windowLength+"s windows and "+windowHop+" window shift (hop) - normalised: "+normalizeEnergy);
        double energyContour [] = nrg.energyCurve(waveFile.getAbsolutePath(), windowLength, windowHop, normalizeEnergy);
        System.out.println("Extracting pitch on "+windowLength+"s windows and "+windowHop+" window shift (hop) with ["+minPitchFreq+";"+maxPitchFreq+"] Hz frequency range");
        double pitchContour [] = pitch.calculatePitch(waveFile.getAbsolutePath(), windowLength, windowHop, minPitchFreq, maxPitchFreq);		
		
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
        String header = "Time_start_s,Time_end_s,energy,pitch";
        bw.write(header+"\n");
        
        for (int i=0;i<energyContour.length;i++) {
        	
        	double t0 = i*windowHop;
        	double t1 = t0+windowLength;
        	double e = energyContour[i];
        	double p = pitchContour[i];
        	
        	String entry = t0+","+t1+","+e+","+p;
        	bw.write(entry);
        	if (i<(energyContour.length-1))
        		bw.write("\n");
        }
        
        
        bw.close();
        System.out.println("Energy-pitch report saved to "+outputFile.getAbsolutePath());
		
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
	                    if (!"true".equalsIgnoreCase(value)
	                            && !"false".equalsIgnoreCase(value)) {
	                        throw new IllegalArgumentException(
	                                "-normalize_energy must be true or false.");
	                    }
	                    normalizeEnergy = Boolean.parseBoolean(value);
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
	                normalizeEnergy);

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
