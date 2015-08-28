/* SHANNON MORAN - 11394476 - 4BCT*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;

public class EvolveMusic {
	
	private static Configuration conf; // Configuration reference object
	private static final String midiFilename = "EvolvedMusic.mid"; // Name of MIDI file evolved by the GA
	private static int fitnessSource = -1; // Fitness function uses this to know which source of fitness to use for evolution
	private static double implicitWeighting = -1; // User specifies weighting to be given to implicit fitness
	private static double explicitWeighting = -1; // User specifies weighting to be given to explicit fitness
	private static double  maxRatio = -1; // Max compression ratio, used in implicit fitness weighting
	private static boolean seedEvolution = false; // Flag to seed evolution
	private static double crThresholdLower = -1; // Lower end compression ratio threshold
	private static double crThresholdHigher = -1; // Higher end compression ratio threshold
	private static ChromosomeFitnessFunction myFitnessFunc = null; // Fitness function reference 
	private static int evolutionIteration = -1; // What iteration of evolution the GA is on
	private static boolean analyseImplicitMapping = false; // Flag to analyse mapping of implicit to explicit fitness
	
	// Constants for the different types of fitness sources
	private static final int EXPLICIT = 1;
	private static final int IMPLICIT = 2;
	private static final int HYBRID = 3;
	private static final int COMBINED = 4;
	
	public static void main(String args[]) {
		// ---------------------------------------------------------------------------------------------
		// --------------------------------* CONFIGURATION SETTINGS *-----------------------------------
		// ---------------------------------------------------------------------------------------------

		// Set GA configuration variables
		int numGenes = 1600; // Number of genes per chromosome
		int numEvolutions = 5; // Set number of evolutions
		int populationSize = 5; // Population size
		
		// Set seed evolution flag
		seedEvolution = true;

		// Set which source of fitness to be used for evolution
		fitnessSource = EXPLICIT;
		
		// ONLY IF IMPLICIT FITNESS, choose whether or not to analyse mapping of implicit fitness to explicit
		analyseImplicitMapping = false;
		
		// ONLY IF COMBINED FITNESS, set implicit and explicit weighting 
		implicitWeighting = 1;
		explicitWeighting = 2;
		
		// ONLY IF COMBINED FITNESS, set compression ratio thresholds
		crThresholdLower = 8;
		crThresholdHigher = 20;
		
		// ---------------------------------------------------------------------------------------------
		// --------------------------------* CONFIGURATION SETTINGS *-----------------------------------
		// ---------------------------------------------------------------------------------------------
		
		// Configure GA, passing in the number of genes to be used to create the template chromosome & population size
		Genotype population = configureGA(numGenes, populationSize);
		
		// Seed the evolution if specified
		IChromosome seedChromosome = null;
		if (seedEvolution) {
			// Get seed chromosome
			try {
				seedChromosome = getSeedChromosome();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			
			// Update population with see chromosome
			population = addSeedChromosomeToPopn(seedChromosome, population);
			System.out.println("\nSeed chromosome added to population\n");
		}

		// Evolve the best solution
		IChromosome chromosome = evolve(population, numEvolutions, populationSize);
		
		// Create MIDI File using this chromosome
	    File midiFile = MidiActions.createMidiFile(chromosome, midiFilename);
	    
	    // Create playback controls
	    MidiActions.createPlaybackControls();
	    
	    // Play MIDI file 
	    MidiActions.playMidiFile(midiFile);
	}
	
	// Evolve
	private static IChromosome evolve(Genotype population, int numEvolutions, int populationSize) {
		
		// Print out starting population
		System.out.println("STARTING POPULATION");
		IChromosome[] startingChromosomes = population.getChromosomes();
		PrintChromosomeGenes.print(startingChromosomes);
	
		System.out.println();
		
		// 2D array of doubles to store all compression ratios for each iteration of evolution and final population
		double[][] compressionRatios = new double[numEvolutions+1][populationSize];
	
		// Evolve population based upon the fitness function set above
		boolean finishedEvolving = false;
		for( int i=0; i<numEvolutions; i++ ) {
			
			// Set variable to signify what iteration of evolution the genetic algorithm is on
			evolutionIteration = (i+1);
			
			System.out.println("\n********************************************************************************************");
			System.out.println("------------------------------------ BEGIN EVOLUTION #"+(i+1)+"------------------------------------");
			System.out.println("********************************************************************************************");
			
			// Check if final iteration of evolution
			if (i == (numEvolutions-1)) 
				finishedEvolving = true;
			
			// Gather compression ratio data for each iteration of evolution for analysis
			// Get max compression ratio to use to weight CR fitness if fitness source uses implicit fitness
			
			// Get chromosomes from current population
			IChromosome[] iterationChromosomes = population.getChromosomes();
			
			// Compare the CR of each chromosome in the population, keep track of the max CR
			double maxCR = -1;
			for (int j=0; j<iterationChromosomes.length; j++) {
				
				// Create MIDI file using this Chromosome to be evaluated 
		    	File midiFile = MidiActions.createMidiFile(iterationChromosomes[j], "compRatioTest.mid");
		    	
				// Get compression ratio of this MIDI file to check for regularity
	        	double compressionRatio = MidiActions.getCompressionRatio(midiFile);
	        	
	        	// Store compression ratio for analysis
	        	compressionRatios[i][j] = compressionRatio;
	        	
	        	// Delete file after getting CR
	        	midiFile.delete();
	        	
	        	// If this CR is greater than current max, set this CR as the max
	        	if (compressionRatio > maxCR) 
	        		maxCR = compressionRatio;
			}
			
			// Set max compression ratio for this iteration of evolution
			maxRatio = maxCR;
						
			// Evolve population
			population.evolve();
			
			// If the final iteration of evolution has happened, store compression ratios of final population
			if (finishedEvolving) {
				// Get chromosomes from current population
				IChromosome[] iterationChromosomes1 = population.getChromosomes();
				
				// Get the CR of each chromosome in the population
				for (int j=0; j<iterationChromosomes1.length; j++) {
					
					// Create MIDI file using this Chromosome to be evaluated 
			    	File midiFile = MidiActions.createMidiFile(iterationChromosomes1[j], "compRatioTest.mid");
			    	
					// Get compression ratio of this MIDI file to check for regularity
		        	double compressionRatio = MidiActions.getCompressionRatio(midiFile);
		        	
		        	// Store compression ratio for analysis
		        	compressionRatios[numEvolutions][j] = compressionRatio;
		        	
		        	// Delete file after getting CR
		        	midiFile.delete();
				}
			}
			
			// New line formatting for each iteration for writing fitness data
			ChromosomeFitnessFunction.newLineFitness();
			
			// If analysing mapping, get user rating for top chromosomes & save data
			if (analyseImplicitMapping) 
				ChromosomeFitnessFunction.getUserRatingsForImplicitMapping();
			
			System.out.println("\n********************************************************************************************");
			System.out.println("---------------------------------- END OF EVOLUTION #"+(i+1)+" ----------------------------------");
			System.out.println("********************************************************************************************\n");
		}
		
		// Close resources for writing fitness data 
		ChromosomeFitnessFunction.closeResourcesFitness();
		
		// If analysing mapping, close resources
		if (analyseImplicitMapping) 
			ChromosomeFitnessFunction.closeResourcesImplicit();

		// Close resources if hybrid fitness source was used 
		if (fitnessSource == HYBRID) ChromosomeFitnessFunction.closeResourcesHybrid();
		
		// Save compression ratio data to file for analysis File and writers
		File compRatioFile = new File("CompressionRatioData.csv");
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			fw = new FileWriter(compRatioFile);
			bw = new BufferedWriter(fw);
		} catch (IOException e) {
			System.out.println("Error creating writers for CompressionRatioData.csv");
			e.printStackTrace();
		}
		
		// Write CR data to file
		try {
			// Write header info
			bw.write("Evolution,");
			for (int b=1; b<=compressionRatios[0].length; b++) {
				bw.write("Chromosome_"+b+",");
			}
			bw.newLine();
			
			// Write data
			for (int a=0; a<compressionRatios.length; a++) {
				// Write evolution iteration number
				bw.write((a+1) + ",");
				
				// Write all CR data for this iteration of evolution
				for (int b=0; b<compressionRatios[a].length; b++) {
					bw.write(compressionRatios[a][b] + ",");
				}
				
				// New line for next iteration
				bw.newLine();
				bw.flush();
			}
		} catch (IOException e) {
			System.out.println("Error writing to CompressionRatioData.csv");
			e.printStackTrace();
		}
		
		// Close resources
		try {
			fw.close();
			bw.close();
		} catch (IOException e) {
			System.out.println("Error closing resources");
			e.printStackTrace();
		}
	
		// Print out population after evolving
		System.out.println("EVOLVED POPULATION");
		IChromosome[] evolvedChromosomes = population.getChromosomes();
		PrintChromosomeGenes.print(evolvedChromosomes);
	
		System.out.println();
	
		// Print out fittest Chromosome
		System.out.println("FITTEST CHROMOSOME & FITNESS");
		IChromosome bestSolutionSoFar = population.getFittestChromosome();
		String bestSolutionChromosomeString = PrintChromosomeGenes.print(bestSolutionSoFar);
		System.out.println(bestSolutionSoFar.getFitnessValue());
		
		// Return fittest chromosome for this iteration
		return bestSolutionSoFar;
	}
	
	// Add seed chromosome to population
	private static Genotype addSeedChromosomeToPopn(IChromosome seedChromosome, Genotype population) {
		
		// Gets initialised with seeded population
		Genotype seededPopulation = null;
		
		// Add seed chromosome to population if specified
		if (seedChromosome != null) {

			// Get initial random population of chromosomes
			IChromosome[] chs = population.getChromosomes();
			
			// Replace random chromosome in array with seed chromosome
			int randomPos = (int) (Math.random()*100)%chs.length;
			chs[randomPos] = seedChromosome;
				
			try {
				// Create new Genotype with seeded population
				seededPopulation = new Genotype(conf, chs);
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
		}
		
		// Return seeded population
		return seededPopulation;
	}
	
	// Get seed chromosome from file 
	private static IChromosome getSeedChromosome() {
		
		BufferedReader br = null;
		IChromosome seedChromosome = null;
		try {
			br = new BufferedReader(new FileReader("seedChromosome.txt"));
			
			// Read line
			String line = br.readLine();
			
			// Split the genes, using comma as separator
			String[] genes = line.split(",");
			
			// Set number of genes in the chromosome	
			// Initialise all genes based upon template
			Gene[] seedGenes = initialiseGenesFromTemplate(new Gene[genes.length]);
			
			// Set each gene's allele value according to seed chromosome values
			for (int i=0; i<seedGenes.length; i++) {
				seedGenes[i].setAllele(Integer.parseInt(genes[i]));
			}
			
			seedChromosome = new Chromosome(conf, seedGenes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Close Resources
			try {
				br.close();
				return seedChromosome;
			} catch (IOException e) {
				System.out.println("Error closing resources");
			}
		}
		
		return seedChromosome;	
	}
	
	// Configure and run genetic algorithm, returning the fittest chromosome according to the fitness function used
	public static Genotype configureGA(int numGenes, int populationSize) {
		
		// Start with DefaultConfiguration, which comes setup with the most common settings
		conf = new DefaultConfiguration();

		// Set the fitness function to use
		myFitnessFunc = new ChromosomeFitnessFunction(crThresholdLower, crThresholdHigher);
		try {
			conf.setFitnessFunction( myFitnessFunc );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		// Set genetic operators
		try {
			conf.addGeneticOperator( new MutationOperator(conf) );
			conf.addGeneticOperator( new CrossoverOperator(conf) );
		} catch (InvalidConfigurationException e1) {
			e1.printStackTrace();
		}

		// Set number of genes in each chromosome	
		// Initialise all genes based upon template
		Gene[] sampleGenes = initialiseGenesFromTemplate(new Gene[numGenes]);

		// Create sample chromosome
		Chromosome sampleChromosome = null;
		try {
			sampleChromosome = new Chromosome(conf, sampleGenes);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		// Set it on the Configuration object as a template for the chromosomes to create
		try {
			conf.setSampleChromosome( sampleChromosome );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		// Set population size
		// The bigger the popn the more potential solutions but takes longer to evolve each iteration
		try {
			conf.setPopulationSize(populationSize);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		
		// Generate random initial population
		Genotype population = null;
		try {
			population = Genotype.randomInitialGenotype(conf);
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		// Return random initial population generated if no seed chromosome is supplied
		return population;
	}
	
	private static Gene[] initialiseGenesFromTemplate(Gene[] sampleGenes) {
		// Calculate max num of genes per phase
		// Minus 4 (3 tempo genes, 1 gene for number of phases); Divide by max number of phases allowed
		int maxGenesPerPhase = (sampleGenes.length-4)/8;

		// Create template IntegerGene objects to represent each of the Genes
		// Set upper and lower bounds on each gene depending on their representation in the chromosome
		// Create the chromosome template with the maximum allowed values for each part of the chromosome
		// Create max of 8 phases for template, with each phase having a specified max length (maxGenesPerPhase)
		// This may result in 'dead DNA' depending on the Gene values specified by each chromosome
		// Dead DNA will be ignored when creating MIDI file
		int count = 0;
		for (int i=0; i<sampleGenes.length; i++) {
			// For each gene, set its bounds depending on its representation in the chromosome
			try {
				if (i<3) {
					// First 3 genes: tempo
					sampleGenes[i] = new IntegerGene(conf, 1, 20);
				} else if (i==3) {
					// Fourth gene: number of phases
					sampleGenes[i] = new IntegerGene(conf, 1, 8);
				} else {
					// Remaining genes: phase data
					if (count%maxGenesPerPhase == 0) {
						// number of times phase is repeated
						sampleGenes[i] = new IntegerGene(conf, 1, 6);
					} else if (count%maxGenesPerPhase == 1) {
						// length of phase (# of Genes)
						sampleGenes[i] = new IntegerGene(conf, 1, maxGenesPerPhase-1);
					} else {
						// offset values
						sampleGenes[i] = new IntegerGene(conf, 1, 30);
					}
					// Increment counter
					count++;
				}
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
		}
		return sampleGenes;
	}
	
	// Getters
	public static int getFitnessSource() {
		return fitnessSource;
	}
	
	public static double getImplicitWeighting() {
		return implicitWeighting;
	}
	
	public static double getExplicitWeighting() {
		return explicitWeighting;
	}

	public static double getMaxRatio() {
		return maxRatio;
	}
	
	public static int getEvolutionIteration() {
		return evolutionIteration;
	}
	
	public static boolean getAnalyseImplicitMapping() {
		return analyseImplicitMapping;
	}
	
}