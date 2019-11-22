package genstar.gamaplugin.operators;

import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.utils.GenStarConstant.SpatialDistribution;
import genstar.gamaplugin.utils.GenStarGamaConstraintBuilder;
import genstar.gamaplugin.utils.GenStarGamaUtils;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.no_test;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;

/**
 * Define all localization operators available
 * 
 * @author kevinchapuis
 *
 */
public class GenstarLocalizeOperators {
	
	// --------------------------------------------------------------------------------
	// MAP ATTRIBUTE OF POPULATION WITH AN ATTRIBUTE OF SPATIAL OBJECT (e.g. census ID)
	// --------------------------------------------------------------------------------
	
	@operator(value = "add_spatial_match", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define an explicit spatial match between synthetic entities and spatial entities (e.g. census area)",
		examples = @example(value = "add_spatial_mapper(my_pop_generator, "
				+ "\"census attribute in synthetic entities\", "
				+ "\"census id in shapefile\", 1#km, 50#m, 0", test = false))
	@no_test
	public static GamaPopGenerator addSpatialMatch(IScope scope, GamaPopGenerator gen, 
			String stringOfCensusIdInCSVfile, String stringOfCensusIdInShapefile, double limit, double step, int priority) {
		if(gen.getPathCensusGeometries() == null ) {
			throw GamaRuntimeException.error("Cannot set a spatial Matcher when the Census Shapefile has not been set.", scope);
		}
		gen.getConstraintBuilder().addLocalizationConstraint(stringOfCensusIdInShapefile, stringOfCensusIdInCSVfile, limit, step, priority);		
		
		return gen;
	}	
	
	@operator(value = "add_spatial_match", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define an explicit spatial match between synthetic entities and spatial entities (e.g. census area)",
		examples = @example(value = "add_spatial_mapper(my_pop_generator, "
				+ "\"census attribute in synthetic entities\", "
				+ "\"census id in shapefile\"", test = false))
	@no_test
	public static GamaPopGenerator addSpatialMatch(IScope scope, GamaPopGenerator gen, 
			String stringOfCensusIdInCSVfile, String stringOfCensusIdInShapefile) {		
		return GenstarLocalizeOperators.addSpatialMatch(scope, gen, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile,0,0,gen.uptadePriorityCounter());
	}	
	
	// --------------------------------------------------------------------------------
	// ADD ANCILARY FILE TO EXTRAPOLATION SPATIAL DISTRIBUTION (Called a MAPPER)
	// --------------------------------------------------------------------------------
	
	@operator(value = "add_ancilary_geofile", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Add a GIS file to perform distribution extrapolation based on a basic mapper (required). It allows to"
			+ " make localization within census area more precise according to ancilary data (e.g. land cover). It can be"
			+ " vector as well as raster GIS file type",
			examples = @example(value = "my_pop_generator add_ancilary_geofile my_file \"path_to_file\"", test = false))
	@no_test
	public static GamaPopGenerator addAncilaryGeoFiles(IScope scope, GamaPopGenerator gen, String pathToFile) {
		gen.addAncilaryGeoFiles(pathToFile);
		return gen;
	}	
	
	// -----------------------------------------------------------
	// DEFINE SPATIAL CONSTRAINT TO FILTER NEST TO BE DRAWN
	// -----------------------------------------------------------
	
	@operator(value = "add_capacity_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint (threshold) to filter acceptable nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint (5,10,2,2)", test = false))
	@no_test
	public static GamaPopGenerator addCapacityConstraint(IScope scope, GamaPopGenerator gen, int capacity, int max, int step, int priority) {
		GenStarGamaConstraintBuilder builder = gen.getConstraintBuilder();
		builder.addCapacityConstraint("", capacity, max, step, priority);
		return gen;
	}
	
	@operator(value = "add_capacity_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint (threshold) to filter acceptable nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint 5", test = false))
	@no_test
	public static GamaPopGenerator addCapacityConstraint(IScope scope, GamaPopGenerator gen, int capacity) {
		return addCapacityConstraint(scope, gen, capacity, capacity, 0, gen.uptadePriorityCounter());
	}
	
	@operator(value = "add_capacity_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint (threshold) to filter acceptable nest based on attribute of the nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint (\"capacity\",100,2,2)", test = false))
	@no_test
	public static GamaPopGenerator addCapacityConstraint(IScope scope, GamaPopGenerator gen, String feature, int max, int step, int priority) {
		GenStarGamaConstraintBuilder builder = gen.getConstraintBuilder();
		builder.addCapacityConstraint(feature, -1, max, step, priority);
		return gen;
	}
	
	@operator(value = "add_capacity_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint (threshold) to filter acceptable nest based on attribute of the nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint (\"capacity\",100,2,2)", test = false))
	@no_test
	public static GamaPopGenerator addCapacityConstraint(IScope scope, GamaPopGenerator gen, String feature) {
		return addCapacityConstraint(scope, gen, feature, 0, 0, gen.uptadePriorityCounter());
	}
	
	@operator(value = "add_density_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint density to filter acceptable nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint (0.2,0.4,0.05,10)", test = false))
	@no_test
	public static GamaPopGenerator addDensityConstraint(IScope scope, GamaPopGenerator gen, double density, double max, double step, int priority) {
		GenStarGamaConstraintBuilder builder = gen.getConstraintBuilder();
		builder.addDensityConstraint("", density, max, step, priority);
		return gen;
	}
	
	@operator(value = "add_density_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint density to filter acceptable nest",
		examples = @example(value = "my_pop_generator add_capacity_constraint 0.2", test = false))
	@no_test
	public static GamaPopGenerator addDensityConstraint(IScope scope, GamaPopGenerator gen, double density) {
		return addDensityConstraint(scope, gen, density, density, 0d, gen.uptadePriorityCounter());
	}
	
	@operator(value = "add_density_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint density to filter acceptable nest based on one of their attributes",
		examples = @example(value = "my_pop_generator add_capacity_constraint (0.2,0.4,0.05,10)", test = false))
	@no_test
	public static GamaPopGenerator addDensityConstraint(IScope scope, GamaPopGenerator gen, String feature, double max, double step, int priority) {
		GenStarGamaConstraintBuilder builder = gen.getConstraintBuilder();
		builder.addDensityConstraint(feature, -1d, max, step, priority);
		return gen;
	}
	
	@operator(value = "add_density_constraint", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a spatial constraint density to filter acceptable nest based on one of their attributes",
		examples = @example(value = "my_pop_generator add_capacity_constraint (0.2,0.4,0.05,10)", test = false))
	@no_test
	public static GamaPopGenerator addDensityConstraint(IScope scope, GamaPopGenerator gen, String feature) {
		return addDensityConstraint(scope, gen, feature, 0d, 0d, gen.uptadePriorityCounter());
	}
	
	// -----------------------------------------------------------
	// DEFINE SPATIAL DISTRIBUTION OF NEST TO DRAW ONE ACCORDINGLY
	// -----------------------------------------------------------
	
	@operator(value = "add_distribution", can_be_const = true, category = {"Gen*"}, concept = { "Gen*"})
	@doc (value = "Define the spatial distribution of nests",
	examples = @example(value = "my_pop_generator add_distribution \"areal\", \"area\"", test = false))
	@no_test
	public static GamaPopGenerator addDistribution(IScope scope, GamaPopGenerator gen, String distribution, String areaFeature) {
		gen.setSpatialDistribution(GenStarGamaUtils.toSpatialDistribution(distribution));
		gen.setSpatialDistributionFeature(areaFeature);
		return gen;
	}
	
	@operator(value = "add_area_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of nests according to area",
		examples = @example(value = "my_pop_generator add_area_distribution \"area\"", test = false))
	@no_test
	public static GamaPopGenerator addAreaDistribution(IScope scope, GamaPopGenerator gen, String areaFeature) {
		gen.setSpatialDistribution(SpatialDistribution.AREA);
		gen.setSpatialDistributionFeature(areaFeature);
		return gen;
	}
	
	@operator(value = "add_capacity_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of capacity type. The capacity is given in each spatial entity by a specified feature",
		examples = @example(value = "my_pop_generator add_capacity_distribution (\"number of inhabitants\")", test = false))
	@no_test
	public static GamaPopGenerator addCapacityDistribution(IScope scope, GamaPopGenerator gen, String featureName) {
		gen.setSpatialDistribution(SpatialDistribution.CAPACITY);
		gen.setSpatialDistributionFeature(featureName);
		return gen;
	}
	
	@operator(value = "add_density_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of density type. "
			+ "The specific density is given in each spatial entity by a specified feature",
		examples = @example(value = "my_pop_generator add_density_distribution (\"density of inhabitants\")", test = false))
	@no_test
	public static GamaPopGenerator addDensityDistribution(IScope scope, GamaPopGenerator gen, String featureName) {
		gen.setSpatialDistribution(SpatialDistribution.DENSITY);
		gen.setSpatialDistributionFeature(featureName);
		return gen;
	}
	
	// -------------------------------------------
	// ALL IN ONE OPERATOR (TO BE VERIFIED THOUGH)
	// -------------------------------------------
	
	@operator(value = "localize_around_at", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a distance based rule to populate agent around: synthetic entities must be at \'max\' distance of "
			+ "the given referent spatial entities (no nest)",
		examples = @example(value = "my_pop_generator localize_around_at 5#m", test = false))
	@no_test
	public static GamaPopGenerator localizeAroundAt(IScope scope, GamaPopGenerator gen, Double max) {
		return localizeAroundAt(scope, gen, 0.0, max, false);
	}		
	
	@operator(value = "localize_around_at", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a distance based rule to populate agent around: synthetic entities must be withing \'min\' and \'max\' "
			+ "ranged distance from the given referent spatial entities (no nest)",
		examples = @example(value = "my_pop_generator localize_around_at (2#m, 10#m)", test = false))
	@no_test
	public static GamaPopGenerator localizeAroundAt(IScope scope, GamaPopGenerator gen, Double min, Double max) {
		return localizeAroundAt(scope,gen,min,max,false);
	}		
	
	@operator(value = "localize_around_at", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define a distance based rule to populate agent around: synthetic entities must be withing \'min\' and \'max\' "
			+ "ranged distance from the given referent spatial entities (no nest). The last argument make it possible to locate"
			+ " people within referent spatial entities",
		examples = @example(value = "my_pop_generator localize_around_at (2#m, 10#m, true)", test = false))
	@no_test
	public static GamaPopGenerator localizeAroundAt(IScope scope, GamaPopGenerator gen, Double min, Double max, boolean overlaps) {
		gen.setLocalizedAround(min, max, overlaps);		
		return gen;
	}	
	
	@operator(value = "localize_on_geometries", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) to a population_generator",
			examples = @example(value = "add_attribute(pop_gen, \"Sex\", string,[\"Man\", \"Woman\"])", test = false))
	@no_test
	public static GamaPopGenerator localizeOnGeometries(IScope scope, GamaPopGenerator gen, String stringPathToGeometriesShapefile) {
		gen.setSpatializePopulation(true);
		gen.setPathNestGeometries(stringPathToGeometriesShapefile);
		return gen;
	}

	@operator(value = "localize_on_census", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) to a population_generator",
			examples = @example(value = "add_attribute(pop_gen, \"Sex\", string,[\"Man\", \"Woman\"])", test = false))
	@no_test
	public static GamaPopGenerator localizeOnCensus(IScope scope, GamaPopGenerator gen, String stringPathToCensusShapefile) {
		gen.setSpatializePopulation(true);
		gen.setPathCensusGeometries(stringPathToCensusShapefile);
		return gen;
	}
}
