package genstar.gamaplugin.utils;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * Define the constant of the Genstar Gama plugin
 * 
 * @author kevinchapuis
 *
 */
public class GenStarConstant {

	/**
	 * Interface to deal with aliases
	 * 
	 * @author kevinchapuis
	 *
	 */
	public interface IGSAlias {
		
		List<String> getAlias();
		
		default boolean getMatch(String alias) {
			return getAlias().stream().anyMatch(elem -> elem.equalsIgnoreCase(alias)); 
		}
		
	}
	
	/**
	 * The spatial distribution to be used in the localization process
	 * 
	 * @author kevinchapuis
	 *
	 */
	public enum SpatialDistribution implements IGSAlias {
		AREA (Arrays.asList("area")),
		CAPACITY (Arrays.asList("capacity"));
		
		List<String> alias;
		
		private SpatialDistribution(List<String> alias) { this.alias = alias; }
		
		@Override
		public List<String> getAlias() { return alias; }
		
	}
	
	/**
	 * The generation algorithms available in the plugin
	 * 
	 * @author kevinchapuis
	 *
	 */
	public enum GenerationAlgorithm implements IGSAlias {
		DIRECTSAMPLING (Arrays.asList("Direct Sampling","DS","IS")), 
		HIERARCHICALSAMPLING (Arrays.asList("Hierarchical Sampling","HS")), 
		UNIFORMSAMPLING (Arrays.asList("Uniform Sampling","US","simple_draw"));
		
		List<String> alias;
		
		private GenerationAlgorithm(List<String> alias) { this.alias = alias; }
		
		@Override
		public List<String> getAlias() { return alias; }
	}
	
	/**
	 * The different type of input data that can be process by Genstar Gama plugin
	 * 
	 * @author kevinchapuis
	 *
	 */
	public enum InputDataType implements IGSAlias {
		CONTINGENCY (Arrays.asList("Contingency","Contingency table","ContingencyTable")),
		FREQUENCY (Arrays.asList("Frequency","Frequency table","FrequencyTable","Global Frequency", "Global Frequency Table","GlobalFrequencyTable")),
		LOCAL (Arrays.asList("Local","Local Frequency Table","LocalFrequency","Local Frequency","LocalFrequencyTable")), 
		SAMPLE (Arrays.asList("Sample","Micro Sample","MicroSample","Micro Data","MicroData"));
		
		List<String> alias;
		
		private InputDataType(List<String> alias) { this.alias = alias; }
		
		@Override
		public List<String> getAlias() { return alias; }
		
	}
	
}
