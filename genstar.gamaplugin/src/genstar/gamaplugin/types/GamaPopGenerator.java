/*********************************************************************************************
 *
 * 'GamaRegression.java, in plugin msi.gama.core, is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 * 
 *
 **********************************************************************************************/

package genstar.gamaplugin.types;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import core.configuration.dictionary.AttributeDictionary;
import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.attribute.record.RecordAttribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.io.GSSurveyWrapper;
import genstar.gamaplugin.utils.GenStarConstant.GenerationAlgorithm;
import genstar.gamaplugin.utils.GenStarConstant.SpatialDistribution;
import msi.gama.common.interfaces.IValue;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.getter;
import msi.gama.precompiler.GamlAnnotations.setter;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaListFactory;
import msi.gama.util.IList;
import msi.gaml.operators.Strings;
import msi.gaml.types.IType;
import msi.gaml.types.Types;
import spin.SpinNetwork;
import spin.SpinPopulation;
import spin.algo.generator.ISpinNetworkGenerator;
import spll.entity.SpllFeature;
import spll.io.SPLVectorFile;
import spll.popmapper.constraint.SpatialConstraintMaxDensity;
import spll.popmapper.constraint.SpatialConstraintMaxNumber;
import spll.popmapper.distribution.ISpatialDistribution;
import spll.popmapper.distribution.SpatialDistributionFactory;


@vars({
	// TODO : old var to clean
	@variable(name = "attributes", type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of attribute names") }),
	@variable(name = "census_files", type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of census files") }), 
	@variable(name = "generation_algo", type = IType.STRING, doc = {@doc("Returns the name of the generation algorithm") }),
	@variable(name = "mappers", type = IType.MAP, doc = {@doc("Returns the list of mapper") }),
	@variable(name = "spatial_file", type = IType.STRING, doc = {@doc("Returns the spatial file used to localize entities") }),
	@variable(name = "spatial_mapper_file", type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of spatial files used to map the entities to areas") }),
	@variable(name = "spatial_matcher_file", type = IType.STRING, doc = {@doc("Returns the spatial file used to match entities and areas") }),
	// New var to include
	@variable(name = GamaPopGenerator.IPF, 
			type = IType.BOOL,
			init = "false",
			doc = {@doc("Enable the use of IPF to extrapolate a joint distribution upon marginals and seed sample")}),
	@variable(name = GamaPopGenerator.FEATURE, 
			type=IType.STRING, 
			doc = {@doc("The spatial feature to setup capacity/density constraint for the spatial distribution")}
			),
	@variable(
			name = GamaPopGenerator.CAPACITYLIMIT,
			type = IType.INT,
			init = "-1",
			doc = @doc ("the capacity limit for capacity spatial constraint relaxation")),
	@variable(
			name = GamaPopGenerator.DENSITYLIMIT,
			type = IType.FLOAT,
			init = "-1.0",
			doc = @doc ("the density limit for density spatial constraint relaxation"))
})
public class GamaPopGenerator implements IValue {

	IPopulation<? extends ADemoEntity, ?> generatedPopulation;
	
	//////////////////////////////////////////////
	// Attirbute for the Gospl generation
	//////////////////////////////////////////////
	
	String generationAlgorithm;
	List<GSSurveyWrapper> inputFiles;
	AttributeDictionary inputAttributes ;
	
	public final static String IPF = "ipf";
	public boolean ipf;
	
	//////////////////////////////////////////////
	// Attirbute for the Spll localization
	//////////////////////////////////////////////
	boolean spatializePopulation;	
	
	String stringOfCensusIdInCSVfile;
	String stringOfCensusIdInShapefile;
	
	String pathCensusGeometries;
	String pathNestedGeometries;

	Double minDistanceLocalize;
	Double maxDistanceLocalize;
	boolean localizeOverlaps;
	
	// Spatial distribution
	public final static String SPATIALDISTRIBUTION = "spatial_distribution";
	private SpatialDistribution spatialDistribution;
	
	public final static String FEATURE = "constraint_feature";
	private String constraintFeature = "";
	
	public final static String CAPACITYCONSTANT = "capacity_constant";
	private double capacityConstraintDistribution = -1d;
	public final static String CAPACITYLIMIT = "capacity_limit";
	private double capacityConstraintLimit = -1d;
	
	public final static String DENSITYCONSTANT = "density_constant";
	private double densityConstraintDistribution = -1d;
	public final static String DENSITYLIMIT = "density_limit";
	private double densityConstraintLimit = -1d;
	
	String crs;

	List<String> pathAncilaryGeofiles;


	//////////////////////////////////////////////
	// Attribute for the Spin generation
	//////////////////////////////////////////////
	Map<String, ISpinNetworkGenerator<? extends ADemoEntity>> networkGenerators;
	Map<ADemoEntity, IAgent> mapEntitiesAgent;


	public GamaPopGenerator() {
		generationAlgorithm = GenerationAlgorithm.DIRECTSAMPLING.getAlias().get(0);		
		inputFiles = new ArrayList<>();
		inputAttributes = new AttributeDictionary();
		
		minDistanceLocalize = 0.0;
		maxDistanceLocalize = 0.0;
		localizeOverlaps = false;
		pathAncilaryGeofiles = new ArrayList<>();	
	
		networkGenerators = new HashMap<>();		
		mapEntitiesAgent = new HashMap<>();
	}
	
	
	@Override
	public String serialize(boolean includingBuiltIn) {
		return null;
	}

	@Override
	public String stringValue(IScope scope) {
		return null;
	}

	@Override
	public IValue copy(IScope scope) {
		return null;
	}

	public AttributeFactory getAttf() {
		return AttributeFactory.getFactory();
	}

	public List<GSSurveyWrapper> getInputFiles() {
		return inputFiles;
	}

	public void setInputFiles(List<GSSurveyWrapper> inputFiles) {
		this.inputFiles = inputFiles;
	}

	public AttributeDictionary getInputAttributes() {
		return inputAttributes;
	}

	public void setInputAttributes(AttributeDictionary inputAttributes) {
		this.inputAttributes = inputAttributes;
	}

	public void setGenerationAlgorithm(String generationAlgorithm) {
		this.generationAlgorithm = generationAlgorithm;
	}

	public void setPathNestedGeometries(String pathGeometries) {
		this.pathNestedGeometries = pathGeometries;
		setSpatializePopulation(Paths.get(pathGeometries).toFile().exists());
	}	

	public String getStringOfCensusIdInCSVfile() {
		return stringOfCensusIdInCSVfile;
	}

	public void setStringOfCensusIdInCSVfile(String stringOfCensusIdInCSVfile) {
		this.stringOfCensusIdInCSVfile = stringOfCensusIdInCSVfile;
	}

	public String getStringOfCensusIdInShapefile() {
		return stringOfCensusIdInShapefile;
	}

	public void setStringOfCensusIdInShapefile(String stringOfCensusIdInShapefile) {
		this.stringOfCensusIdInShapefile = stringOfCensusIdInShapefile;
	}

	public boolean isSpatializePopulation() {
		return spatializePopulation;
	}

	public void setSpatializePopulation(boolean spatializePopulation) {
		this.spatializePopulation = spatializePopulation;
	}

	public String getCrs() {
		return crs;
	}

	public void setCrs(String crs) {
		this.crs = crs;
	}
	
	@getter("attributes")
	public IList<String> getAttributeName(){
		IList<String> atts = GamaListFactory.create(Types.STRING);
		for (Attribute<? extends core.metamodel.value.IValue> a : this.getInputAttributes().getAttributes())
			atts.add(a.getAttributeName());
		return atts;
	}
	
	@getter("census_files")
	public IList<String> getCensusFile(){
		IList<String> f = GamaListFactory.create(Types.STRING);
		for (GSSurveyWrapper a : this.getInputFiles()) f.add(a.getRelativePath().toString());
		return f;
	}
	
	@getter("generation_algo")
	public String getGenerationAlgorithm() { return generationAlgorithm; }
	
	@getter(IPF)
	public boolean getIPF() { return this.ipf; }
	
	@setter(IPF)
	public void setIPF(boolean ipf) { this.ipf = ipf; }
	
	@getter("spatial_file")
	public String getPathNestedGeometries() {
		return pathNestedGeometries;
	}

	public Collection<RecordAttribute<Attribute<? extends core.metamodel.value.IValue>, 
				Attribute<? extends core.metamodel.value.IValue>>> getRecordAttributes() {
		return inputAttributes.getRecords();
		
	}

	public void setSpatialMapper(String stringOfCensusIdInCSVfile, String stringOfCensusIdInShapefile) {
		this.stringOfCensusIdInCSVfile = stringOfCensusIdInCSVfile;
		this.stringOfCensusIdInShapefile = stringOfCensusIdInShapefile;
	}

	public void setPathCensusGeometries(String stringPathToCensusShapefile) {
		this.pathCensusGeometries = stringPathToCensusShapefile;
		
		setSpatializePopulation(pathCensusGeometries != null);
	}

	public String getPathCensusGeometries() {
		return pathCensusGeometries;
	}


	public void setLocalizedAround(Double min, Double max, boolean overlaps) {
		setMinDistanceLocalize(min);
		setMaxDistanceLocalize(max);
		setLocalizeOverlaps(overlaps);
	}


	public Double getMinDistanceLocalize() {
		return minDistanceLocalize;
	}


	public void setMinDistanceLocalize(Double minDistanceLocalize) {
		this.minDistanceLocalize = minDistanceLocalize;
	}


	public Double getMaxDistanceLocalize() {
		return maxDistanceLocalize;
	}


	public void setMaxDistanceLocalize(Double maxDistanceLocalize) {
		this.maxDistanceLocalize = maxDistanceLocalize;
	}


	public boolean isLocalizeOverlaps() {
		return localizeOverlaps;
	}


	public void setLocalizeOverlaps(boolean localizeOverlaps) {
		this.localizeOverlaps = localizeOverlaps;
	}

	// --------------------
	// Spatial distribution
	// --------------------
	
	@getter(SPATIALDISTRIBUTION)
	public SpatialDistribution getSpatialDistribution() { return spatialDistribution; }
	
	@setter(SPATIALDISTRIBUTION)
	public void setSpatialDistribution(SpatialDistribution spatialDistribution) { this.spatialDistribution = spatialDistribution; }

	@getter(FEATURE)
	public String getSpatialDistributionFeature() {return constraintFeature;}	
	
	@setter(FEATURE)
	public void setSpatialDistributionFeature(String feature) { this.constraintFeature = feature; }
	
	@getter(CAPACITYCONSTANT)
	public int getSpatialDsitrbutionCapacity() {return (int) this.capacityConstraintDistribution;}
	
	@setter(CAPACITYCONSTANT)
	public void setSpatialDistributionCapacity(int capacity) { this.capacityConstraintDistribution = capacity; }
	
	@getter(CAPACITYLIMIT)
	public int getSpatialDistributionCapacityLimit() {return (int) this.capacityConstraintLimit;}
	
	@setter(CAPACITYLIMIT)
	public void setSpatialDistributionCapacityLimit(int limit) {this.capacityConstraintLimit = limit;}
	
	@getter(DENSITYCONSTANT)
	public double getSpatialDistributionDensity() {return this.densityConstraintDistribution;}
	
	@setter(DENSITYCONSTANT)
	public void setSpatialDistributionDensity(double density) { this.densityConstraintDistribution = density; }
	
	@getter(DENSITYLIMIT)
	public int getSpatialDistributionDensityLimit() {return (int) this.densityConstraintLimit;}
	
	@setter(DENSITYLIMIT)
	public void setSpatialDistributionDensityLimit(double limit) {this.densityConstraintLimit = limit;}
	
	@SuppressWarnings("rawtypes")
	public ISpatialDistribution getSpatialDistribution(SPLVectorFile sfGeometries, IScope scope) {		
		if(getSpatialDistribution() == null) {setSpatialDistribution(SpatialDistribution.DEFAULT);}
		switch(getSpatialDistribution().getConcept()) {
			case NUMBER :  
				SpatialConstraintMaxNumber scmn = null;
				boolean featureBased = false;
				if (constraintFeature != null && !Strings.isEmpty(constraintFeature)) {
					List<SpllFeature> sf = sfGeometries.getGeoEntity().stream().filter(f -> f.getAttributes()
							.stream().noneMatch(af -> af.getAttributeName().equalsIgnoreCase(constraintFeature)))
							.collect(Collectors.toList());
					if(!sf.isEmpty()) {
						throw GamaRuntimeException.error("The specified capacity constraint feature "
							+constraintFeature+" is not present in "+Arrays.asList(sf).toString(), scope);
					}
					featureBased = true;
				}
				switch(getSpatialDistribution()) {
					case CAPACITY :
						if(featureBased) {
							scmn = new SpatialConstraintMaxNumber(sfGeometries.getGeoEntity(), constraintFeature);
						} else {
							if(capacityConstraintDistribution <= 0) {capacityConstraintDistribution = 1;}
							scmn = new SpatialConstraintMaxNumber(sfGeometries.getGeoEntity(), capacityConstraintDistribution);
						}
						if (getSpatialDistributionCapacityLimit() > 0) { scmn.setMaxIncrease(getSpatialDistributionCapacityLimit()); }
						break;
					case DENSITY :
						if(featureBased) {
							scmn = new SpatialConstraintMaxDensity(sfGeometries.getGeoEntity(), constraintFeature);
						} else if(capacityConstraintDistribution > 0) {
							scmn = new SpatialConstraintMaxDensity(sfGeometries.getGeoEntity(), densityConstraintDistribution);
						} else {
							throw GamaRuntimeException.error(new IllegalArgumentException("A density or feature name must be specified for "
									+ "a density based constraint spatial distribution").getMessage(), scope);
						}
						if (getSpatialDistributionDensityLimit() > 0) { scmn.setMaxIncrease(getSpatialDistributionDensityLimit()); }
						break;
					default: break;
				}
				return SpatialDistributionFactory.getInstance().getCapacityBasedDistribution(scmn);
			case COMPLEX :
				throw GamaRuntimeException.error(new UnsupportedOperationException("Complex spatial distribution "
						+ "have not been yet passed in the plugin").getMessage(), scope);
			case SIMPLE : 
			default :
				switch(getSpatialDistribution()) {
					case AREA : 
						return SpatialDistributionFactory.getInstance().getAreaBasedDistribution(sfGeometries);
					default :
						return SpatialDistributionFactory.getInstance().getUniformDistribution();
				}
		}
	}
	

	public List<String> getPathAncilaryGeofiles() {
		return pathAncilaryGeofiles;
	}

	public void addAncilaryGeoFiles(String pathToFile) {
		pathAncilaryGeofiles.add(pathToFile);
	}

	public void setPathAncilaryGeofiles(List<String> pathAncilaryGeofiles) {
		this.pathAncilaryGeofiles = pathAncilaryGeofiles;
	}


	public boolean isSocialPopulation() {
		return !networkGenerators.isEmpty();
	}

	public void addNetworkGenerator(String graphName, ISpinNetworkGenerator<? extends ADemoEntity> graphGenerator) {
		networkGenerators.put(graphName, graphGenerator);
	}
	
	public Map<String, ISpinNetworkGenerator<? extends ADemoEntity>> getNetworkGenerators() {
		return networkGenerators;
	}


	public void setNetworksGenerator(Map<String, ISpinNetworkGenerator<? extends ADemoEntity>> networksGenerator) {
		this.networkGenerators = networksGenerator;
	}

	public ISpinNetworkGenerator<? extends ADemoEntity> getNetworkGenerator(String networkName) {
		return networkGenerators.get(networkName);
	}	
	
	public SpinNetwork getNetwork(String networkName) {
		if(isSocialPopulation()) {
			return ((SpinPopulation<?>)generatedPopulation).getNetwork(networkName);	
		} else {
			return null;
		}
	}

	public void setGeneratedPopulation(IPopulation<? extends ADemoEntity, ?> population) {
		this.generatedPopulation = population;
	}
	public IPopulation<? extends ADemoEntity, ?> getGeneratedPopulation() {
		return generatedPopulation;
	}
	
	public void addAgent(ADemoEntity e, IAgent a) {
		mapEntitiesAgent.put(e, a);	
	}

	public IAgent getAgent(ADemoEntity e) {
		return mapEntitiesAgent.get(e);
	}
	
	public Collection<IAgent> getAgents() {
		return mapEntitiesAgent.values();
	}

}
