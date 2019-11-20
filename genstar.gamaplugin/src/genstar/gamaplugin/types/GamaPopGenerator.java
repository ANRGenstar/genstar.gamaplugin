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
import genstar.gamaplugin.utils.GenStarGamaConstraintBuilder;
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
import spll.popmapper.constraint.ISpatialConstraint;
import spll.popmapper.constraint.SpatialConstraintMaxDensity;
import spll.popmapper.constraint.SpatialConstraintMaxNumber;
import spll.popmapper.distribution.ISpatialDistribution;
import spll.popmapper.distribution.SpatialDistributionFactory;


@vars({
	// TODO : old var to clean
	@variable(name = GamaPopGenerator.ATTRIBUTES, type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of attribute names") }),
	@variable(name = "census_files", type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of census files") }), 
	@variable(name = "generation_algo", type = IType.STRING, doc = {@doc("Returns the name of the generation algorithm") }),
	@variable(name = "mappers", type = IType.MAP, doc = {@doc("Returns the list of mapper") }),
	
	@variable(name = "spatial_mapper_file", type = IType.LIST, of = IType.STRING, doc = {@doc("Returns the list of spatial files used to map the entities to areas") }),
	@variable(name = "spatial_matcher_file", type = IType.STRING, doc = {@doc("Returns the spatial file used to match entities and areas") }),
	// New var to include
	@variable(name = GamaPopGenerator.NESTS, 
		type = IType.STRING, 
		doc = {@doc("Returns the spatial file used to localize entities") }),
	@variable(name = GamaPopGenerator.IPF, 
		type = IType.BOOL,
		init = "false",
		doc = {@doc("Enable the use of IPF to extrapolate a joint distribution upon marginals and seed sample")}),
	@variable(name = GamaPopGenerator.D_FEATURE, 
		type=IType.STRING, 
		doc = {@doc("The spatial feature to based spatial distribution of nest uppon")}),
	@variable(name = GamaPopGenerator.C_FEATURE, 
		type=IType.STRING, 
		doc = {@doc("The spatial feature to setup capacity/density constraint to filter acceptable nests")}),

})
public class GamaPopGenerator implements IValue {

	IPopulation<? extends ADemoEntity, ?> generatedPopulation;
	
	//////////////////////////////////////////////
	// Attirbute for the Gospl generation
	//////////////////////////////////////////////
	
	public final static String GENERATION_ALGO = "GOSP_algorithm";
	String generationAlgorithm;
	
	public final static String DEMOGRAPHIC_FILES = "demographic_files";
	List<GSSurveyWrapper> inputFiles;
	
	public final static String ATTRIBUTES_DICTIONARY = "demographic_dictionary";
	public final static String ATTRIBUTES = "demographic_attributes";
	AttributeDictionary inputAttributes;
	
	public final static String IPF = "ipf";
	public boolean ipf;
	
	//////////////////////////////////////////////
	// Attirbute for the Spll localization
	//////////////////////////////////////////////
	boolean spatializePopulation;	
	
	String stringOfCensusIdInCSVfile;
	String stringOfCensusIdInShapefile;
	
	String pathCensusGeometries;
	
	public final static String NESTS = "Nests_geometries";
	String pathNestGeometries;

	Double minDistanceLocalize;
	Double maxDistanceLocalize;
	boolean localizeOverlaps;
	
	// Spatial distribution
	public final static String SPATIALDISTRIBUTION = "spatial_distribution";
	private SpatialDistribution spatialDistribution;
	
	public final static String D_FEATURE = "distribution_feature";
	private String distributionFeature = "";
	
	public final static String C_FEATURE = "constraint_feature";
	private String constraintFeature = "";

	public final static String CONSTRAINTS = "spatial_constraints";
	private GenStarGamaConstraintBuilder cBuilder;
	
	String crs;

	List<String> pathAncilaryGeofiles;


	//////////////////////////////////////////////
	// Attribute for the Spin generation
	//////////////////////////////////////////////
	Map<String, ISpinNetworkGenerator<? extends ADemoEntity>> networkGenerators;
	Map<ADemoEntity, IAgent> mapEntitiesAgent;

	
	/**
	 * Default constructor
	 */
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
		
		cBuilder = new GenStarGamaConstraintBuilder();
	}
	
	// IValue constract
	
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

	// ACCESSOR FOR SP GENERATION
	
	public AttributeFactory getAttf() {
		return AttributeFactory.getFactory();
	}

	@setter(DEMOGRAPHIC_FILES)
	public void setInputFiles(List<GSSurveyWrapper> inputFiles) {
		this.inputFiles = inputFiles;
	}

	@getter(ATTRIBUTES_DICTIONARY)
	public AttributeDictionary getInputAttributes() {
		return inputAttributes;
	}

	@setter(ATTRIBUTES_DICTIONARY)
	public void setInputAttributes(AttributeDictionary inputAttributes) {
		this.inputAttributes = inputAttributes;
	}
	
	@getter(ATTRIBUTES)
	public IList<String> getAttributeName(){
		IList<String> atts = GamaListFactory.create(Types.STRING);
		for (Attribute<? extends core.metamodel.value.IValue> a : this.getInputAttributes().getAttributes())
			atts.add(a.getAttributeName());
		return atts;
	}
	
	@getter(DEMOGRAPHIC_FILES)
	public List<GSSurveyWrapper> getInputFiles() {
		return inputFiles;
	}
	
	public IList<String> getCensusFile(){
		IList<String> f = GamaListFactory.create(Types.STRING);
		for (GSSurveyWrapper a : this.getInputFiles()) f.add(a.getRelativePath().toString());
		return f;
	}
	
	@getter(GENERATION_ALGO)
	public String getGenerationAlgorithm() { return generationAlgorithm; }
	
	public GenerationAlgorithm getGenstarGenerationAlgorithm() {return GenerationAlgorithm.getAlgorithm(this.generationAlgorithm); }
	
	@getter(IPF)
	public boolean getIPF() { return this.ipf; }
	
	@setter(IPF)
	public void setIPF(boolean ipf) { this.ipf = ipf; }

	public void setGenerationAlgorithm(String generationAlgorithm) {
		this.generationAlgorithm = generationAlgorithm;
	}

	// ACCESSOR FOR SP LOCALIZATION
	
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
	
	@getter(NESTS)
	public String getPathNestGeometries() {
		return this.pathNestGeometries;
	}
	
	@setter(NESTS)
	public void setPathNestGeometries(String path) {
		this.pathNestGeometries = path;
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

	@getter(D_FEATURE)
	public String getSpatialDistributionFeature() {return distributionFeature;}	
	
	@setter(D_FEATURE)
	public void setSpatialDistributionFeature(String feature) { this.distributionFeature = feature; }
	
	@SuppressWarnings("rawtypes")
	public ISpatialDistribution getSpatialDistribution(SPLVectorFile sfGeometries, IScope scope) {		
		if(getSpatialDistribution() == null) {setSpatialDistribution(SpatialDistribution.DEFAULT);}
		switch(getSpatialDistribution().getConcept()) {
			case NUMBER :  
				SpatialConstraintMaxNumber scmn = null;
				if (constraintFeature != null && !Strings.isEmpty(constraintFeature)) {
					List<SpllFeature> sf = sfGeometries.getGeoEntity().stream().filter(f -> f.getAttributes()
							.stream().noneMatch(af -> af.getAttributeName().equalsIgnoreCase(constraintFeature)))
							.collect(Collectors.toList());
					if(!sf.isEmpty()) {
						throw GamaRuntimeException.error("The specified capacity constraint feature "
							+constraintFeature+" is not present in "+Arrays.asList(sf).toString(), scope);
					}
				} else {
					throw GamaRuntimeException.error("You must specified a spatial feature (attribute) to based distribution upon", scope);
				}
				switch(getSpatialDistribution()) {
					case CAPACITY :
						scmn = new SpatialConstraintMaxNumber(sfGeometries.getGeoEntity(), constraintFeature);
						break;
					case DENSITY :
						scmn = new SpatialConstraintMaxDensity(sfGeometries.getGeoEntity(), constraintFeature);
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
	
	
	// ------------------
	// Spatial constraint
	// ------------------
	
	@getter(CONSTRAINTS)
	public GenStarGamaConstraintBuilder getConstraintBuilder() {
		return this.cBuilder;
	}
	
	public Collection<ISpatialConstraint> getConstraints(SPLVectorFile sfGeometries, IScope scope) throws IllegalStateException {
		return this.cBuilder.buildConstraints(sfGeometries.getGeoEntity());
	}
	
	// -------------------------
	// Mapper / ancilary methods
	// -------------------------
	
	public List<String> getPathAncilaryGeofiles() {
		return pathAncilaryGeofiles;
	}

	public void addAncilaryGeoFiles(String pathToFile) {
		pathAncilaryGeofiles.add(pathToFile);
	}

	public void setPathAncilaryGeofiles(List<String> pathAncilaryGeofiles) {
		this.pathAncilaryGeofiles = pathAncilaryGeofiles;
	}

	// -----------------------
	// Network related methods
	// -----------------------

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
