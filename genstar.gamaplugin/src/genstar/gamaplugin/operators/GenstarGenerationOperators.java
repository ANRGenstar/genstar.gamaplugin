package genstar.gamaplugin.operators;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.operation.TransformException;

import core.configuration.GenstarConfigurationFile;
import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.entity.AGeoEntity;
import core.metamodel.io.IGSGeofile;
import core.metamodel.value.IValue;
import core.util.excpetion.GSIllegalRangedData;
import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.utils.GenStarGamaUtils;
import gospl.GosplEntity;
import gospl.GosplPopulation;
import gospl.algo.IGosplConcept.EGosplAlgorithm;
import gospl.algo.ipf.SRIPFAlgo;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.algo.sr.ds.DirectSamplingAlgo;
import gospl.algo.sr.hs.HierarchicalHypothesisAlgo;
import gospl.distribution.GosplContingencyTable;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.ACoordinate;
import gospl.generator.DistributionBasedGenerator;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.sampler.IDistributionSampler;
import gospl.sampler.IHierarchicalSampler;
import gospl.sampler.ISampler;
import gospl.sampler.sr.GosplBasicSampler;
import gospl.sampler.sr.GosplHierarchicalSampler;
import msi.gama.metamodel.shape.GamaShape;
import msi.gama.metamodel.shape.IShape;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.no_test;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaListFactory;
import msi.gama.util.GamaMapFactory;
import msi.gama.util.IList;
import msi.gaml.operators.Spatial;
import msi.gaml.types.Types;
import spin.SpinPopulation;
import spin.algo.generator.ISpinNetworkGenerator;
import spll.SpllEntity;
import spll.SpllPopulation;
import spll.algo.LMRegressionOLS;
import spll.algo.exception.IllegalRegressionException;
import spll.datamapper.exception.GSMapperException;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLRasterFile;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.popmapper.SPLocalizer;
import spll.popmapper.normalizer.SPLUniformNormalizer;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenstarGenerationOperators {


	@operator(value = "with_generation_algo", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "define the algorithm used for the population generation among: IS (independant hypothesis Algorothm) and simple_draw (simple draw of entities in a sample)",
			examples = @example(value = "my_pop_generator with_generation_algo \"simple_draw\"", test = false))
	@no_test
	public static GamaPopGenerator withGenerationAlgo(GamaPopGenerator gen, String algo) {
		if (gen == null) {
			gen = new GamaPopGenerator();
		}
		gen.setGenerationAlgorithm(algo);
		return gen;
	}
	
	/**
	 * Main methods to generate a population from a {@link GamaPopGenerator} with a given number of synthetic entities
	 * 
	 * @param scope
	 * @param gen
	 * @param targetPopulation
	 * @return
	 */
	public static IPopulation<ADemoEntity, Attribute<? extends IValue>> generatePop(final IScope scope, GamaPopGenerator gen, Integer targetPopulation) {
		if (gen == null) {
			return null;
		}

		Path baseDirectory = FileSystems.getDefault().getPath(".");		
		
		GenstarConfigurationFile confFile = new GenstarConfigurationFile();
		confFile.setBaseDirectory(baseDirectory);
		confFile.setSurveyWrappers(gen.getInputFiles());
		confFile.setDictionary(gen.getInputAttributes());

		////////////////////////////////////////////////////////////////////////
		// Gospl generation
		////////////////////////////////////////////////////////////////////////
		
		GosplInputDataManager gdb = new GosplInputDataManager(confFile);
		
		IPopulation<ADemoEntity, Attribute<? extends IValue>> population = new GosplPopulation();
		
		final EGosplAlgorithm algo = GenStarGamaUtils.toGosplAlgorithm(gen.getGenerationAlgorithm());
		switch (algo.concept) {
		case SR: // SYNTHETIC RECONSTRUCTION
			try {
				gdb.buildDataTables();  // Load and read input data
			} catch (final RuntimeException | IOException | InvalidSurveyFormatException | InvalidFormatException e) {
				throw GamaRuntimeException.error("Error in building dataTable for the IS algorithm. "+e.getMessage(), scope);
			}

			INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> distribution = null;
			try {
				distribution = gdb.collapseDataTablesIntoDistribution(); // Build a distribution from input data
			} catch (final IllegalDistributionCreation e1) {
				throw GamaRuntimeException.error("Error of distribution creation in collapsing DataTable into distibution. "+e1.getMessage(), scope);
			} catch (final IllegalControlTotalException e1) {
				throw GamaRuntimeException.error("Error of control in collapsing DataTable into distibution. "+e1.getMessage(), scope);
			}

			// BUILD THE SAMPLER WITH THE PROPER ALGORITHM
			ISampler<ACoordinate<Attribute<? extends IValue>, IValue>> sampler = null;

			switch (algo) { 
			case HS: // HIERARCHICAL SAMPLING
				ISyntheticReconstructionAlgo<IHierarchicalSampler> hierarchicalInfAlgo = new HierarchicalHypothesisAlgo();
				try {
					sampler = hierarchicalInfAlgo.inferSRSampler(distribution, new GosplHierarchicalSampler());
				} catch (IllegalDistributionCreation e2) {
					throw GamaRuntimeException.error(e2.getLocalizedMessage(),scope);
				}
				break;
			case DS: // DIRECT SAMPLING | DEFAULT
			default:
				ISyntheticReconstructionAlgo<IDistributionSampler> distributionInfAlgo = null;
				if (gen.ipf) {
					try {
						gdb.buildSamples();
					} catch (final IOException | InvalidSurveyFormatException 
							| InvalidFormatException e) {
						throw GamaRuntimeException.error(e.getLocalizedMessage(), scope);
					}

					// Input sample
					IPopulation<ADemoEntity, Attribute<? extends IValue>> seed = gdb.getRawSamples().stream()
							.findFirst().orElseThrow(NullPointerException::new);

					// Setup IPF with seed, number of maximum fitting iteration, and delta convergence criteria 
					distributionInfAlgo = new SRIPFAlgo(seed, 100, Math.pow(10, -4));
				} else { 
					distributionInfAlgo = new DirectSamplingAlgo();
				}
				try {
					sampler = distributionInfAlgo.inferSRSampler(distribution, new GosplBasicSampler());
				} catch (final IllegalDistributionCreation e1) {
					throw GamaRuntimeException.error("Error of distribution creation in infering the sampler for "+algo.name
							+" SR Based algorithm. "+e1.getMessage(), scope);
				}
				break;
			}

			// DEFINE THE POPULATION SIZE
			if (targetPopulation < 0) {
				int min = Integer.MAX_VALUE;
				for (INDimensionalMatrix<Attribute<? extends IValue>,IValue,? extends Number> mat: gdb.getRawDataTables()) {
					if (mat instanceof GosplContingencyTable) {
						GosplContingencyTable cmat = (GosplContingencyTable) mat;
						min = Math.min(min, cmat.getMatrix().values().stream().mapToInt(v -> v.getValue()).sum());
					}
				}
				if (min < Integer.MAX_VALUE) {
					targetPopulation =min;
				} else targetPopulation = 1;
			}
			targetPopulation = targetPopulation <= 0 ? 1 : targetPopulation;

			// BUILD THE POPULATION
			population = new DistributionBasedGenerator(sampler).generate(targetPopulation);
			break;
			
		case CO: // COMBINATORIAL OPTIMIZATION 
			try {
				gdb.buildSamples(); // Retrieve sample
			} catch (final RuntimeException | IOException | InvalidSurveyFormatException | InvalidFormatException e) {
				throw GamaRuntimeException.error(e.getLocalizedMessage(), scope);
			} 

			IPopulation p = gdb.getRawSamples().stream().findFirst().orElseThrow(NullPointerException::new);

			switch (algo) { 

			case SA: // SIMULATED ANNEANLING
				throw new UnsupportedOperationException(EGosplAlgorithm.SA.name
						+" based combinatorial optimization population synthesis have not yet been ported from API to plugin ! "
						+ "if necessary, requests dev at https://github.com/ANRGenstar/genstar.gamaplugin ;)");
			case TABU: // TABU SEARCH
				throw new UnsupportedOperationException(EGosplAlgorithm.TABU.name
						+" based combinatorial optimization population synthesis have not yet been ported from API to plugin ! "
						+ "if necessary, requests dev at https://github.com/ANRGenstar/genstar.gamaplugin ;)");
			case RS: // RANDOM SEARCH
				throw new UnsupportedOperationException(EGosplAlgorithm.RS.name
						+" based combinatorial optimization population synthesis have not yet been ported from API to plugin ! "
						+ "if necessary, requests dev at https://github.com/ANRGenstar/genstar.gamaplugin ;)");
			case US : // UNIFORM & RAW SAMPLING | DEFAULT
			default :
				if (targetPopulation <= 0) {
					population = p;
				} else {
					List<ADemoEntity> popSample = new ArrayList<>(p);
					for (int i= 0; i < targetPopulation; i++) {
						ADemoEntity ent =  popSample.get(scope.getRandom().between(0, popSample.size()-1));
						Map<Attribute<? extends IValue>, IValue> atts = ent.getAttributes().stream()
								.collect(Collectors.toMap(a -> a, a -> ent.getValueForAttribute(a)));
						ADemoEntity entity = new GosplEntity(atts);
						population.add(entity);
					}
				}

				break;
			}
			break;

			// -----------------
			// TODO : UPDATE POPULATION
			// -----------------
		case MIXTURE: 
			throw new UnsupportedOperationException("Mixture population synthesis have not yet been ported from API to plugin ! request dev if necessary ;)");

			// --------------------------------
			// TODO : MULTILEVEL POPULATION GENERATION
			// --------------------------------
		case MULTILEVEL:
			throw new UnsupportedOperationException("Genstar Gama plugin Cannot yet build a multi-level population");
		}
       
		if (population == null) return null;
       
       ////////////////////////////////////////////////////////////////////////
       // Spll generation
       //////////////////////////////////////////////////////////////////////// 
       
       if (gen.isSpatializePopulation())
			population = spatializePopulation(scope, gen, population);
      
       ////////////////////////////////////////////////////////////////////////
       // Spin generation
       ////////////////////////////////////////////////////////////////////////
       
       if (gen.isSocialPopulation())
    	   population = socializePopulation(gen, population);
       
		return population;
	}


	@operator(value = "generate_localized_entities", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "generate a spatialized population taking the form of a list of geometries while trying to infer the entities number from the data", 
		examples = @example(value = "generateLocalizedEntities(my_pop_generator)", test = false))
	@no_test
	public static IList<IShape> generateLocalizedEntities(final IScope scope,GamaPopGenerator gen) {
		return generateLocalizedEntities(scope,gen, null);
	}
	
	@operator(value = "generate_localized_entities", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "generate a population composed of the given number of entities taking the form of a list of geometries", 
		examples = @example(value = "generateLocalizedEntities(my_pop_generator, 1000)", test = false))
	@no_test
	public static IList<IShape> generateLocalizedEntities(final IScope scope,GamaPopGenerator gen, Integer number) {
		if (number == null) number = -1;
		IPopulation<? extends ADemoEntity, Attribute<? extends IValue>> population = generatePop(scope, gen, number);
		IList<IShape> entities =  GamaListFactory.create(Types.GEOMETRY);
		if (gen == null) return entities;
		final Collection<Attribute<? extends IValue>> attributes = population.getPopulationAttributes();
	   int nb = 0;
        List<ADemoEntity> es = new ArrayList(population);
        if (number > 0 && number < es.size()) es = scope.getRandom().shuffle(es);
        for (final ADemoEntity e : es) {
        	IShape entity = null;
        	if (population instanceof SpllPopulation) {
        		SpllEntity newE = (SpllEntity) e;
        		if (newE.getLocation() == null) continue;
        		entity = new GamaShape(gen.getCrs() != null ?
						Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation()), gen.getCrs())
						: Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation())));
        	} else {
        		entity = new GamaShape(Spatial.Punctal.any_location_in(scope, scope.getRoot().getGeometry()));
        	}
        	
        	for (final Attribute<? extends IValue> attribute : attributes) {
        		 entity.setAttribute(attribute.getAttributeName(), GenStarGamaUtils.toGAMAValue(scope,e.getValueForAttribute(attribute), true));
        	  }
        	
            entities.add(entity);
            nb ++;
            if (number > 0 && nb >= number) break;
        }	
		return entities;
	}

	
	public static IList<IShape> genPop(IScope scope, IPopulation<? extends ADemoEntity, Attribute<? extends IValue>> population, String crs, int number) {
		IList<IShape> entities =  GamaListFactory.create(Types.GEOMETRY);
		final Collection<Attribute<? extends IValue>> attributes = population.getPopulationAttributes();
	    int nb = 0;
        List<ADemoEntity> es = new ArrayList(population);
        if (number > 0 && number < es.size()) es = scope.getRandom().shuffle(es);
        for (final ADemoEntity e : es) {
        	IShape entity = null;
        	if (population instanceof SpllPopulation) {
        		SpllEntity newE = (SpllEntity) e;
        		if (newE.getLocation() == null) continue;
        		entity = new GamaShape(crs != null ? 
						Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation()), crs)
						: Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation())));
        	} else {
        		entity = new GamaShape(Spatial.Punctal.any_location_in(scope, scope.getRoot().getGeometry()));
        	}
        	
        	for (final Attribute<? extends IValue> attribute : attributes)
                entity.setAttribute(attribute.getAttributeName(), GenStarGamaUtils.toGAMAValue(scope, e.getValueForAttribute(attribute), true));
            entities.add(entity);
            nb ++;
            if (number > 0 && nb >= number) break;
        }	
		return entities;
	}
	
	
	@operator(value = "generate_entities", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "generate a population taking the form of of a list of map (each map representing an entity) "
			+ "while trying to infer the entities number from the data", 
			examples = @example(value = "generate_entities(my_pop_generator)", test = false))
	@no_test
	public static IList<Map> generateEntities(final IScope scope,GamaPopGenerator gen) {
		return generateEntities(scope, gen, null);
	}
	
	@operator(value = "generate_entities", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "generate a population composed of the given number of entities taking the form of a list of map: each map representing an entity", 
		examples = @example(value = "generate_entities(my_pop_generator, 1000)", test = false))
	@no_test
	public static IList<Map> generateEntities(final IScope scope,GamaPopGenerator gen, Integer number) {
		if (number == null) number = -1;
		IPopulation<? extends ADemoEntity, Attribute<? extends IValue>> population = generatePop(scope, gen, number);
		IList<Map> entities =  GamaListFactory.create(Types.MAP);
		if (gen == null) return entities;
		final Collection<Attribute<? extends IValue>> attributes = population.getPopulationAttributes();
      
        int nb = 0;
        List<ADemoEntity> es = new ArrayList(population);
        if (number > 0 && number < es.size()) es = scope.getRandom().shuffle(es);
        for (final ADemoEntity e : es) {
        	
        	Map entity = GamaMapFactory.create();
        	 		
        	for (final Attribute<? extends IValue> attribute : attributes) {
                final String name = attribute.getAttributeName();
                entity.put(name, GenStarGamaUtils.toGAMAValue(scope, e.getValueForAttribute(attribute), true));

            	if (population instanceof SpllPopulation) {
            		SpllEntity newE = (SpllEntity) e;
            		if (newE.getLocation() != null) {
            			entity.put("location", new GamaShape(new GamaShape(gen.getCrs() != null ?
								Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation()), gen.getCrs())
								: Spatial.Projections.to_GAMA_CRS(scope, new GamaShape(newE.getLocation())))));
            		}
            	}
            }
            entities.add(entity);
            nb ++;
            if (number > 0 && nb >= number) break;
        }	
		return entities;
	}
	
	
	private static IPopulation spatializePopulation(IScope scope, GamaPopGenerator gen, IPopulation population) {
		File sfGeomsF = gen.getPathNestedGeometries() == null ? null : new File(gen.getPathNestedGeometries());
		File sfCensusF = gen.getPathCensusGeometries() == null ? null : new File(gen.getPathCensusGeometries());

		SPLVectorFile sfGeoms = null;
		SPLVectorFile sfCensus = null;		
		
		if (sfGeomsF != null && !sfGeomsF.exists()) return population;			
	 				
		try {
			if(sfGeomsF != null) {
				
				// Take usable attribute from the file, e.g. for capacity constraint based spatial distribution
				List<String> att = gen.getSpatialDistributionFeature().isEmpty() || gen.getSpatialDistributionFeature().equals("") ? 
						new ArrayList<>() : Arrays.asList(gen.getSpatialDistributionFeature());
				
				sfGeoms = SPLGeofileBuilder.getShapeFile(sfGeomsF, att, null);
				if(gen.getMaxDistanceLocalize() > 0.0) {
					sfGeoms.minMaxDistance(gen.getMinDistanceLocalize(), gen.getMaxDistanceLocalize(), gen.isLocalizeOverlaps());				
				}
				// TODO limité aux geom ? ou aussi au census ?
				gen.setCrs(sfGeoms.getWKTCoordinateReferentSystem());				
			}
			
			if(sfCensusF != null) {			
				sfCensus = SPLGeofileBuilder.getShapeFile(sfCensusF, null); 
			}
		} catch (IOException | InvalidGeoFormatException | GSIllegalRangedData e) {
			throw GamaRuntimeException.error(e.getMessage(), scope);
		}
		
		// SETUP THE LOCALIZER
		SPLocalizer localizer;		
		if(sfGeoms != null) {
			localizer = new SPLocalizer(population, sfGeoms);	
			localizer.setDistribution(gen.getSpatialDistribution(sfGeoms, scope));
		} else {
			localizer = new SPLocalizer(population, sfCensus);
			localizer.setDistribution(gen.getSpatialDistribution(sfCensus, scope));			
		}
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		if (sfCensus != null) {
			localizer.setMatcher(sfCensus, gen.getStringOfCensusIdInCSVfile(), gen.getStringOfCensusIdInShapefile());				
		}				

		// SETUP REGRESSION
		List<IGSGeofile<? extends AGeoEntity<? extends IValue>, ? extends IValue>> endogeneousVarFile = new ArrayList<>();
		for(String path : gen.getPathAncilaryGeofiles()){
			try {
				File pathF = new File(path);
				if (pathF.exists())
					endogeneousVarFile.add(new SPLGeofileBuilder().setFile(pathF).buildGeofile());
			} catch ( IOException | IllegalArgumentException | TransformException | InvalidGeoFormatException | GSIllegalRangedData e) {
				throw GamaRuntimeException.error("Error in setuping the regression. "+e.getMessage(), scope);
			}
		}		
		
		if (!endogeneousVarFile.isEmpty())
			try {
				// TODO cntingency ID ????? 
//				if (gen.getSpatialContingencyId() != null && !gen.getSpatialContingencyId().isEmpty()) {
//					localizer.setMapper(endogeneousVarFile.get(0), gen.getSpatialContingencyId());				
//				}
//				else 
				if (sfCensus != null)
					localizer.setMapper(endogeneousVarFile, new ArrayList<>(), new LMRegressionOLS(), new SPLUniformNormalizer(0, SPLRasterFile.DEF_NODATA));
				
			} catch (IndexOutOfBoundsException | IllegalRegressionException 
					| IllegalArgumentException | IOException | TransformException 
					| ExecutionException | GSMapperException 
					| SchemaException | InvalidGeoFormatException e) {
				throw GamaRuntimeException.error(e.getMessage(), scope);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} 
		
		//localize the population
		return localizer.localisePopulation();		
	}
	
	
	private static IPopulation socializePopulation(GamaPopGenerator gen, IPopulation population) {
		SpinPopulation socializedPop = new SpinPopulation<>(population);
		
		for(Entry<String,ISpinNetworkGenerator<? extends ADemoEntity>> e : gen.getNetworkGenerators().entrySet()) {
			socializedPop.addNetwork(e.getKey(), e.getValue().generate(population));
		}	
		
		return socializedPop;
	}	
	
}
