/* SHANNON MORAN - 11394476 - 4BCT*/

import org.jgap.IChromosome;

/**	
 * Prints out the String representation of the genes of the inputed Chromosome(s)
 */

public abstract class PrintChromosomeGenes {
	
	public static void print(IChromosome[] chromosomes) {
		for(IChromosome c : chromosomes) {
			String chromosomeString = "";
			for (int i=0; i<c.getGenes().length; i++)
				chromosomeString += (int)c.getGene(i).getAllele()+",";

			System.out.println(chromosomeString);
		}
	}

	public static String print(IChromosome chromosome) {
		String chromosomeString = "";
		for (int i=0; i<chromosome.getGenes().length; i++)
			chromosomeString += (int)chromosome.getGene(i).getAllele()+",";

		System.out.println(chromosomeString);
		return chromosomeString;
	}
	
	public static String getChromosomeString(IChromosome chromosome) {
		String chromosomeString = "";
		for (int i=0; i<chromosome.getGenes().length; i++)
			chromosomeString += (int)chromosome.getGene(i).getAllele()+",";

		return chromosomeString;
	}
}
