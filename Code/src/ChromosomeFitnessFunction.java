/* SHANNON MORAN - 11394476 - 4BCT*/

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

public class ChromosomeFitnessFunction extends FitnessFunction {

	private static double userFitness = -1; // Fitness rating obtained from user feedback
	private static boolean selectedFitness = false; // Check if rating has been given
	private static double CRTHRESHOLDLOWER; // Lower end compression ratio threshold 
	private static double CRTHRESHOLDHIGHER; // Higher end compression ratio threshold 
	private static IChromosome[] fittestChromosomesForMapping = new IChromosome[3]; // For storing fittest chromosomes when analysing implicit mapping to explicit
	private static double[] fitnessValuesForMapping = new double[3]; // For storing fitness of fittest chromosomes when analysing implicit mapping to explicit
	
	// For storing fitness related values
	private static BufferedWriter bw_fitness = null;
	private static FileWriter fw_fitness = null;
	private static File fitnessFile = null;
	
	// For storing when human involvement occurs in hybrid fitness
	private static File humanInvolvement = null;
	private static boolean firstHybridCall = true;
	private static BufferedWriter bw_hybrid = null;
	private static FileWriter fw_hybrid = null;
	
	// For storing mapping data for implicit to explicit fitness
	private static File implicitMappingFile = null;
	private static boolean firstImplicitCall = true;
	private static BufferedWriter bw_implicit = null;
	private static FileWriter fw_implicit = null;

	// Constructor
    public ChromosomeFitnessFunction(double crThresholdLower, double crThresholdHigher) {
    	// Set CR threshold bounds on initialisation
    	CRTHRESHOLDLOWER = crThresholdLower;
    	CRTHRESHOLDHIGHER = crThresholdHigher;
    	
    	// Initialise writers to write to fitness data file
    	fitnessFile = new File("FitnessData.csv");
		fw_fitness = null;
		bw_fitness = null;
		try {
			fw_fitness = new FileWriter(fitnessFile);
			bw_fitness = new BufferedWriter(fw_fitness);
		} catch (IOException e) {
			System.out.println("Error creating writers for FitnessData.csv");
			e.printStackTrace();
		}
    }

    /**	
     * Determine the fitness of the given chromosome based upon a user specified source of fitness.
     * The higher the return value, the more fit the chromosome. 
     *
     * @param chromosome - The chromosome to evaluate
     *
     * @return A positive double reflecting the fitness rating of the given chromosome
     */
    public double evaluate(IChromosome chromosome) {
    	    	
    	// Create MIDI file using this Chromosome to be evaluated 
    	File midiFile = MidiActions.createMidiFile(chromosome, "fitness.mid");

    	// Get fitness source to be used
    	int fitnessSource = EvolveMusic.getFitnessSource();
    	double fitness = -1;
    	
    	// Calculate fitness based on specified source 
    	switch (fitnessSource) {
    	
	    	case 1: // EXPLICIT - user feedback
	    		// Play MIDI file
    			boolean playOK = MidiActions.playMidiFile(midiFile);
    			
    			// Get user fitness rating
    			if (playOK)
    				getUserFitnessRating();
    			
    			// Calculate user fitness based on weighting
	    		fitness = calculateExplicitFitness(userFitness);

    			// Release resources
    			MidiActions.closeSequencer();
	    		break;
	    		
	    	case 2: // IMPLICIT - regularity
	    		
	    		// If analysing mapping, create file and initialise writers if the evolution is just beginning
	    		if (EvolveMusic.getAnalyseImplicitMapping() && firstImplicitCall) {
	    			firstImplicitCall = false; // Reset boolean
	    			
	    			implicitMappingFile = new File("ImplicitFitnessMappingData.csv");
		    		try{
		    			fw_implicit = new FileWriter(implicitMappingFile);
		    			bw_implicit = new BufferedWriter(fw_implicit);
		    			
		    			// Write headers
		    			bw_implicit.write("Error_Rate_1,Error_Rate_2,Error_Rate_3,Average Error Rate,");
		    			bw_implicit.newLine();
		    			bw_implicit.flush();
		    		} catch (IOException e) {
		    			System.out.println("Error creating writers for ImplicitFitnessMappingData.csv");
		    			e.printStackTrace();
		    		}
	    		}
	    		
	    		// Get compression ratio of this MIDI file to check for regularity
	        	double compressionRatio = MidiActions.getCompressionRatio(midiFile);
	        	
	        	System.out.println("\nCR: " + compressionRatio);
	        	
	    		fitness = calculateImplicitFitness(compressionRatio, EvolveMusic.getMaxRatio());
	    		
	    		// If the mapping of implicit to explicit fitness is being analysed
	    		if (EvolveMusic.getAnalyseImplicitMapping()) {
	    			
	    			// Loop array of fitness values
	    			for (int i=0; i<fitnessValuesForMapping.length; i++) {
	    				// If current fitness is greater than any fitness in array, update with new fitness value and associated chromosome
	    				if (fitness > fitnessValuesForMapping[i]) {
	    					fitnessValuesForMapping[i] = fitness;
	    					fittestChromosomesForMapping[i] = chromosome;
	    					break; // break when this chromosome has been added to the array of best chromosomes
	    				}
	    			}
	    			
	    		}
	    		
	    		break;
	    	
	    	case 3: // HYBRID - either feedback or regularity
	    		
	    		// Create file and initialise writers if the evolution is just beginning
	    		if (firstHybridCall) {
	    			firstHybridCall = false; // Reset boolean
	    			
	    			humanInvolvement = new File("HumanInvolvementData.csv");
		    		try{
		    			fw_hybrid = new FileWriter(humanInvolvement);
		    			bw_hybrid = new BufferedWriter(fw_hybrid);
		    		} catch (IOException e) {
		    			System.out.println("Error creating writers for HumanInvolvementData.csv");
		    			e.printStackTrace();
		    		}
	    		}
	    		
	    		// Get compression ratio of this MIDI file to check for regularity
	        	double compressionRatio1 = MidiActions.getCompressionRatio(midiFile);
	        	
	        	System.out.println("\nCR: " + compressionRatio1);
	        	
	        	// If CR is below the lower end CR threshold, or above the higher end threshold, a user fitness rating is acquired
	        	// The optimal CR is within a range, as too random is considered bad AND too regular is considered bad
	        	if (compressionRatio1<CRTHRESHOLDLOWER || compressionRatio1>CRTHRESHOLDHIGHER) {
	    			// Play MIDI file
	    			boolean playOK1 = MidiActions.playMidiFile(midiFile);
	    			
	    			// Get user fitness rating
	    			if (playOK1)
	    				getUserFitnessRating();
	    			
	    			// Calculate user fitness based on weighting
		    		fitness = calculateExplicitFitness(userFitness);
		    		
		    		// Write to file to signify that an explicit fitness was used in this iteration of evolution
		    		try {
						bw_hybrid.write(EvolveMusic.getEvolutionIteration()+": "+fitness+",");
						bw_hybrid.flush();
					} catch (IOException e) {
						System.out.println("Error writing to HumanInvolvementData.csv");
						e.printStackTrace();
					}
	    			
	    			// Release resources
	    			MidiActions.closeSequencer();
	        	}
	        	// Else give it an automatic fitness rating based on CR value
	        	else {
		    		fitness = calculateImplicitFitness(compressionRatio1, EvolveMusic.getMaxRatio());
	        	}
	        	break;
	    	
	    	case 4: // COMBINED - feedback and regularity
	    		// Get compression ratio of this MIDI file to check for regularity
	        	double compressionRatio2 = MidiActions.getCompressionRatio(midiFile);
	        	
	        	System.out.println("\nCR: " + compressionRatio2);
	        	
	        	// Play MIDI file
    			boolean playOK1 = MidiActions.playMidiFile(midiFile);
    			
    			// Get user fitness rating
    			if (playOK1)
    				getUserFitnessRating();
    			
    			// Release resources
    			MidiActions.closeSequencer();
	    		
	        	// Calculate combined fitness
	    		fitness = calculateCombinedFitness(compressionRatio2, EvolveMusic.getMaxRatio(), userFitness, EvolveMusic.getImplicitWeighting(), EvolveMusic.getExplicitWeighting());
	    		break;
    	}

    	// Delete file after getting a fitness rating for it
    	midiFile.delete();
    	
    	System.out.println("\nFITNESS: " + fitness);

    	// Output this fitness data to file
    	try {
			bw_fitness.write(fitness + ",");
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	// Return fitness
    	return fitness;
    }
    
    // Create radio buttons and get user fitness rating
    private static void getUserFitnessRating() {
    	
    	selectedFitness = false;
    	
    	// Create JFrame with GridBagLayout
    	final JFrame frame = new JFrame("Rating");
    	frame.setLocationRelativeTo( null ); // Centre on screen
        frame.setLayout(new GridBagLayout());
        
        // Set constrains for each button
        GridBagConstraints c = new GridBagConstraints();
        //c.fill = GridBagConstraints.HORIZONTAL;
    	
        // Create radio buttons
        JRadioButton button1 = new JRadioButton("1");
        c.weightx = 0.2;
        c.gridy = 0;
        c.gridx = 1;
        frame.add(button1, c);
        
        JRadioButton button2 = new JRadioButton("2");
        c.weightx = 0.2;
        c.gridy = 0;
        c.gridx = 2;
        frame.add(button2, c);
        
        JRadioButton button3 = new JRadioButton("3");
        c.weightx = 0.2;
        c.gridy = 0;
        c.gridx = 3;
        frame.add(button3, c);

        JRadioButton button4 = new JRadioButton("4");
        c.weightx = 0.2;
        c.gridy = 0;
        c.gridx = 4;
        frame.add(button4, c);

        JRadioButton button5 = new JRadioButton("5");
        c.weightx = 0.2;
        c.gridy = 0;
        c.gridx = 5;
        frame.add(button5, c);

        //Group the radio buttons so that only one button in the group can be selected at a time
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(button1);
        radioGroup.add(button2);
        radioGroup.add(button3);
        radioGroup.add(button4);
        radioGroup.add(button5);
        
        // Create play buttons
        JButton stopButton = new JButton("[ ]");
        stopButton.setActionCommand("stop");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 20; // Button py padding
        c.insets = new Insets(20,0,0,0);  // Padding between radio buttons and control buttons
        c.gridy = 1;
        c.gridx = 2;
        frame.add(stopButton, c);
        
        // Create stop button 
        JButton playButton = new JButton("I>");
        playButton.setActionCommand("play");
        c.gridy = 1;
        c.gridx = 4;
        frame.add(playButton, c);

        // Exit program on close of JFrame as a rating is necessary to continue
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create inner class for the action listener for the radio buttons
        class RadioListener implements ActionListener{
        	public void actionPerformed(ActionEvent e){
        		// Create JRadioButton object from the button that was selected
        		JRadioButton button = (JRadioButton) e.getSource();
        		
        		// Check which button was selected
        		userFitness = Double.parseDouble(button.getActionCommand());
        		
        		// Stop playing the MIDI file data after getting a rating
    	    	MidiActions.stopSequencer();
        		
    	    	// Close on selection
        		frame.setVisible(false);
        		
        		// Allows the program to continue after getting a rating
        		selectedFitness = true;  		
        	}
        }
        
        // Create inner class for the action listener for the radio buttons
        class ControlListener implements ActionListener{
        	public void actionPerformed(ActionEvent e){
        		// Get action command
        		String action = e.getActionCommand();
        		
        		// Carry out specific button action depending on what button was pressed
        		if (action.equals("stop")) {
        			MidiActions.stopSequencer();
        		} else if (action.equals("play")) {
        			MidiActions.startSequencer();
        		}
        	}
        }
        
        // Create listener and add it to radio buttons
        RadioListener listener = new RadioListener();
        button1.addActionListener(listener);
        button2.addActionListener(listener);
        button3.addActionListener(listener);
        button4.addActionListener(listener);
        button5.addActionListener(listener);
        
        // Create listener and add it to control buttons
        ControlListener controlListener = new ControlListener();
        stopButton.addActionListener(controlListener);
        playButton.addActionListener(controlListener);
        //pauseButton.addActionListener(controlListener);

        // Set size, layout and visibility
        frame.setSize(350,200);
        //frame.setLayout( new FlowLayout());
        frame.setVisible(true);
        
        // Wait for fitness value to be given before continuing
        try {
        	while (selectedFitness == false) {
        		Thread.sleep(200);
        	}
		} catch (InterruptedException e) {
			System.out.println("Interruption Error while waiting for fitness value to be given");
		}
    }
    
    private static double calculateImplicitFitness(double ratio, double maxRatio) {
		// Calculate fitness based on weighting
    	return ratio/maxRatio;
    }
    
    private static double calculateExplicitFitness(double feedback) {
		// Calculate fitness based on weighting
    	return feedback/5;
    }
    
    private static double calculateCombinedFitness(double ratio, double maxRatio, double feedback, double alpha, double beta) {
		// Calculate fitness based on weighting
    	return ((alpha*(ratio/maxRatio))+(beta*(feedback/5)) / (alpha+beta));
    }
    
    public static void closeResourcesFitness() {
    	// Close resources
 		try {
 			fw_fitness.close();
 			bw_fitness.close();
 		} catch (IOException e) {
 			System.out.println("Error closing resources");
 			e.printStackTrace();
 		}
    }
    
    public static void newLineFitness() {
    	// Write new line and flush buffer
 		try {
 			bw_fitness.newLine();
 			bw_fitness.flush();
 		} catch (IOException e) {
 			System.out.println("Error closing resources");
 			e.printStackTrace();
 		}
    }
    
    public static void closeResourcesHybrid() {
    	// Close resources
 		try {
 			fw_hybrid.close();
 			bw_hybrid.close();
 		} catch (IOException e) {
 			System.out.println("Error closing resources");
 			e.printStackTrace();
 		}
    }
    
    public static void getUserRatingsForImplicitMapping() {
    	// For top chromosomes: get user rating & store data to file
    	
    	double errorRateTotal = 0;
    	// Get user rating for each chromosme and write to file
    	for (int i=0; i<fittestChromosomesForMapping.length; i++) {
	    	// Create MIDI file using this Chromosome to be evaluated 
	    	File midiFile = MidiActions.createMidiFile(fittestChromosomesForMapping[i], "implicitMapping.mid");
	    	
	    	// Play MIDI file
			boolean playOK = MidiActions.playMidiFile(midiFile);
			
			// Get user fitness rating
			if (playOK)
				getUserFitnessRating();
			
			// Calculate user fitness based on weighting
			double explicitFitness = calculateExplicitFitness(userFitness);
	
			// Release resources
			MidiActions.closeSequencer();
			
			// Delete file after getting a fitness rating for it
	    	midiFile.delete();
	    	
	    	// Write data to file
	    	try {
	    		// Get error rate and write it to file
	    		double errorRate = fitnessValuesForMapping[i]-explicitFitness;
	    		errorRateTotal += errorRate; // Add to errorRateTotal for average calculation
	    		bw_implicit.write(errorRate+","); // Write implicit - explicit, to get error rate
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	
    	// Newline for next evolution iteration
    	try {
    		// Write average error rate
    		double averageErrorRate = errorRateTotal/3;
    		bw_implicit.write(averageErrorRate+",");
    		
    		// New line
			bw_implicit.newLine();
			bw_implicit.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void closeResourcesImplicit() {
    	// Close resources
 		try {
 			fw_implicit.close();
 			bw_implicit.close();
 		} catch (IOException e) {
 			System.out.println("Error closing resources");
 			e.printStackTrace();
 		}
    }
}