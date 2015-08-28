/* SHANNON MORAN - 11394476 - 4BCT*/

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.jgap.IChromosome;


/**	
 * Creates a MIDI file based upon the inputed Chromosome
 */

public abstract class MidiActions {
	
	private static Sequencer sequencer; // Sequencer to control MIDI file playback
	private static final int BUFFERSIZE = 2048; // Buffer size constant used for file compression

	// Create MIDI file
	public static File createMidiFile(IChromosome chromosome, String filename) {
		
		// Midi Objects
		MetaMessage mt;
	    ShortMessage mm;
	    MidiEvent me;
	    
	    // Get String representation of chromosome to use to create a MIDI file
		String chromosomeString = PrintChromosomeGenes.getChromosomeString(chromosome);
			    
		// Create and populate String array with each position having a gene value of the chromosome
		String[] chromosomeArray = chromosomeString.split(",");
	    
		// File that will reference MIDI file when created
		File midiFile = null;
		try
		{
			// Create a new MIDI sequence with 24 ticks per beat
			Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ,24);
	
			// Obtain a MIDI track from the sequence
			Track t = s.createTrack();

			// General MIDI sysex (system exclusive) -- turn on General MIDI sound set
			byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
			SysexMessage sm = new SysexMessage();
			sm.setMessage(b, 6);
			me = new MidiEvent(sm,(long)0);
			t.add(me);
			
			// Set tempo (meta event)  
			mt = new MetaMessage();
			int tempo1 = Integer.parseInt(chromosomeArray[0], 10);
			int tempo2 = Integer.parseInt(chromosomeArray[1], 10);
			int tempo3 = Integer.parseInt(chromosomeArray[2], 10);
			byte[] bt = {(byte) tempo1, (byte)tempo2, (byte)tempo3}; // Num microseconds per quarter note
			mt.setMessage(0x51 ,bt, 3);
			me = new MidiEvent(mt,(long)0);
			t.add(me);
			
			System.out.println("\n-----------------------------------------------------------------------------");
			System.out.println("Tempos: "+tempo1+" "+tempo2+" "+tempo3+"; BPM: "+tempo1*tempo2*tempo3+"\n");
	
			// Set track name (meta event)
			mt = new MetaMessage();
			String TrackName = new String("midifile track");
			mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
			me = new MidiEvent(mt,(long)0);
			t.add(me);

			// Get number of phases
			int numPhases = Integer.parseInt(chromosomeArray[3], 10);
			System.out.println("Num phases: "+numPhases);
			
			// Counters
			int nextBytePosition = 4;
			int beatTime = 0;
			
			OutOfBoundsBreak:
			for (int i=1; i<=numPhases; i++) {
				// For each phase:
				// - number of times it is repeated
				// - length of phase (# bytes)
				// - remaining bytes -> offset amount of each beat from the previous beat in the phase
				
				int numRepeats = Integer.parseInt(chromosomeArray[nextBytePosition], 10);
				int lengthPhase = Integer.parseInt(chromosomeArray[nextBytePosition+1], 10);
				
				System.out.println("Phase "+i+" - Num repeats: "+numRepeats+"; Length of phase: "+lengthPhase);
				
				// Increment position counter for new phase
				nextBytePosition+=2;
				
				// Repeat phase a number of times specified by the chromosome
				for (int r=0; r<numRepeats; r++) {
					// Reset start point at each iteration of repetition of this phase 
					int byteStartPoint = nextBytePosition;
					
					// Read beats starting from the start point in the array
					// Until specified number of beats have been read for this phase
					for (int q=0; q<lengthPhase; q++) {
						
						if ((byteStartPoint+q)<chromosomeArray.length) { // Out of bounds check
							
							int beatOffset = Integer.parseInt(chromosomeArray[byteStartPoint+q], 10);
							beatOffset %= 30;
							
							beatTime = beatTime+beatOffset;
							
							// Note on - Electric Snare - 40
							mm = new ShortMessage();
							mm.setMessage(0x99, 0x28, 0x40);
							me = new MidiEvent(mm,(long)beatTime);
							t.add(me);
							
							// Increment beatTime for the note off event
							// Ensures each beat is played and not overlapping with next beat
							beatTime+=20;
							
							// Note off - Electric Snare - 40
							mm = new ShortMessage();
							mm.setMessage(0x89, 0x28, 0x00);
							me = new MidiEvent(mm,(long)beatTime);
							t.add(me);
							
						} else {
							break OutOfBoundsBreak;
						}
					}
				}
				
				// Calculate max number of genes per phase based upon the number of genes in the chromosome
				// Minus 4 (3 tempo genes, 1 gene for number of phases); Divide by max number of phases allowed
				int maxGenesPerPhase = (chromosome.getGenes().length-4)/8;
				
				// Increment position counter after current phase
				// Increment by the value of maxGenesPerPhase value
				// This implements the ignoring of dead DNA
				nextBytePosition+=(maxGenesPerPhase-2);
			}
			
			// Set end of track (meta event)
			mt = new MetaMessage();
	        byte[] bet = {}; // empty array
			mt.setMessage(0x2F,bet,0);
			me = new MidiEvent(mt, (long)beatTime);
			t.add(me);
	
			// Write the MIDI sequence to a MIDI file 
			midiFile = new File(filename);
			MidiSystem.write(s,1,midiFile);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		// Return File
		return midiFile;
	}

	//Play MIDI file
	public static boolean playMidiFile(File midiFile) {
		try {
			// Get default Sequencer connected to default device
	    	sequencer = MidiSystem.getSequencer();
	    	
	    	// Opens the device acquiring any system resources it requires 
	    	sequencer.open();
	    	
	    	// Create input stream to read in MIDI file data
	    	InputStream in = new BufferedInputStream(new FileInputStream(midiFile));
	    	
	    	// Set the sequence to operate on, pointing to the MIDI file data in the input stream
	    	sequencer.setSequence(in);
	    	
	    	// Start playing the MIDI file data
	    	sequencer.start();
		} catch (MidiUnavailableException e) {
			System.out.println("Error accessing MIDI file to play: unavailable");
			return false;
		} catch (FileNotFoundException e) {
			System.out.println("Error finding MIDI file to play: file not found");
			return false;
		} catch (IOException e) {
			System.out.println("I/O Error while reading MIDI file to play");
			return false;
		} catch (InvalidMidiDataException e) {
			System.out.println("Error trying to play MIDI file: invalid midi data");
			return false;
		}
	
		// MIDI file played successfully
		return true;
	}
	
	// Close sequencer
	public static void closeSequencer() {
		sequencer.close();
	}
	
	// Start sequencer
	public static void startSequencer() {
		sequencer.start();
	}
	
	// Stop sequencer
	public static void stopSequencer() {
		sequencer.stop();
	}
	
	public static void createPlaybackControls() {
		// Create JFrame with GridBagLayout
    	final JFrame frame = new JFrame("Rating");
    	frame.setLocationRelativeTo( null ); // Centre on screen
        frame.setLayout(new GridBagLayout());
        
        // Set constrains for each button
        GridBagConstraints c = new GridBagConstraints();
		
		// Create stop buttons
        JButton stopButton = new JButton("[ ]");
        stopButton.setActionCommand("stop");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 20; // Button py padding
        c.insets = new Insets(20,0,0,0);  // Padding between radio buttons and control buttons
        c.gridy = 1;
        c.gridx = 2;
        frame.add(stopButton, c);
        
        // Create play button
        JButton playButton = new JButton("I>");
        playButton.setActionCommand("play");
        c.gridy = 1;
        c.gridx = 4;
        frame.add(playButton, c);


        // Exit program on close of JFrame as a rating is necessary to continue
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
			// Create inner class for the action listener for the radio buttons
	        class ControlListener implements ActionListener{
	        	public void actionPerformed(ActionEvent e){
	        		// Create JButton object from the button that was selected
	        		JButton button = (JButton) e.getSource();
	        		
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
	        
	     // Create listener and add it to control buttons
        ControlListener controlListener = new ControlListener();
        stopButton.addActionListener(controlListener);
        playButton.addActionListener(controlListener);
        //pauseButton.addActionListener(controlListener);

        // Set size, layout and visibility
        frame.setSize(350,200);
        //frame.setLayout( new FlowLayout());
        frame.setVisible(true);
	}
	
	// Calculate compression ratio of MIDI file
    public static double getCompressionRatio(File midiFile) {
    	
    	// Constant for the name of the zip file
    	final String ZIPFILENAME = "compressedFitness.zip";
    	
    	// Get size of uncompressed file
    	double uncompressedSize = midiFile.length();
    	System.out.println("\nUncompressed file size: "+uncompressedSize);
    	
    	// Create output streams to write to zip file
    	FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(ZIPFILENAME);
		} catch (FileNotFoundException e) {
			System.out.println("Error creating ZIP file");
			//e.printStackTrace();
		}
    	ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
    	
    	// Set method of zipping to compress the file
    	zos.setMethod(ZipOutputStream.DEFLATED);

    	// Create input streams to read in data that is to be written to ZIP file
    	FileInputStream fis = null;
		try {
			fis = new FileInputStream(midiFile);
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find file to put into ZIP file");
			//e.printStackTrace();
		}
    	BufferedInputStream bis = new BufferedInputStream(fis, BUFFERSIZE);
    	
    	// Create ZipEntry with filename to be added
    	ZipEntry ze = new ZipEntry(midiFile.getName()); 
    	
    	// Begins writing a new ZIP file entry and positions the stream to the start of the entry data
    	try {
			zos.putNextEntry(ze);
		} catch (IOException e) {
			System.out.println("Error adding next entry to ZIP file");
			//e.printStackTrace();
		} 
    	
    	// Byte array to hold data read in by stream
    	byte[] data = new byte[BUFFERSIZE];
    	
    	// Write data to ZIP file
    	int count;
    	try {
    		// Until end of data, read data into byte array with specified offset and length 
			while((count = bis.read(data, 0, BUFFERSIZE)) != -1) {
				//
				zos.write(data, 0, count);
			}
		} catch (IOException e) {
			System.out.println("Error writing to ZIP file");
			//e.printStackTrace();
		}
    	
    	try {
    		bis.close();
			zos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	// Get size of compressed file
    	File compressed = new File(ZIPFILENAME);
    	double compressedSize = compressed.length();
    	System.out.println("Compressed file size: "+compressedSize);
    	
    	// Calculate CR by getting the ratio between the uncompressed size and compressed size
    	double compressionRatio = uncompressedSize/compressedSize;
    	System.out.println("Compression ratio: "+compressionRatio+"\n");
    	
    	// Delete ZIP file after calculating the CR
    	compressed.delete();
    	
    	// Return CR
    	return compressionRatio;
    }
    
}
