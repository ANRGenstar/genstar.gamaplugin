package genstar.gamaplugin.operators;

import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.utils.GenStarConstant.SpatialDistribution;
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
	
	@operator(value = "add_spatial_mapper", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define an explicit spatial mapping between synthetic entities and spatial entities (e.g. census area)",
		examples = @example(value = "add_spatial_mapper(my_pop_generator, "
				+ "\"census attribute in synthetic entities\", "
				+ "\"census id in shapefile\"", test = false))
	@no_test
	public static GamaPopGenerator addSpatialMapper(IScope scope, GamaPopGenerator gen, 
			String stringOfCensusIdInCSVfile, String stringOfCensusIdInShapefile) {
		if(gen.getPathCensusGeometries() == null ) {
			throw GamaRuntimeException.error("Cannot set a spatial Mapper when the Census Shapefile has not been set.", scope);
		}
		gen.setSpatialMapper(stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);		
		
		return gen;
	}	
	
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
	
	@operator(value = "add_spatial_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution to be used to localize synthetic entities",
		examples = @example(value = "my_pop_generator add_spatial_distribution \"area\"", test = false))
	@no_test
	public static GamaPopGenerator addSpatialDistribution(IScope scope, GamaPopGenerator gen, String distribution) {
		SpatialDistribution sd = GenStarGamaUtils.toSpatialDistribution(distribution);
		if(sd == null){throw GamaRuntimeException.error("The spatial distribution "+distribution
				+" does not match any Gen* spatial distribution - see", scope);}
		gen.setSpatialDistribution(sd);
		return gen;
	}
	
	@operator(value = "add_capacity_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of capacity type with a constant number of maximum synthetic entities per spatial entity",
		examples = @example(value = "my_pop_generator add_capacity_distribution 5", test = false))
	@no_test
	public static GamaPopGenerator addCapacityDistribution(IScope scope, GamaPopGenerator gen, int capacity) {
		gen.setSpatialDistribution(SpatialDistribution.CAPACITY);
		gen.setSpatialDistributionCapacity(capacity);
		return gen;
	}
	
	@operator(value = "add_capacity_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of capacity type. The capacity is given in each spatial entity by a specified feature",
		examples = @example(value = "my_pop_generator add_capacity_distribution \"number of inhabitants\"", test = false))
	@no_test
	public static GamaPopGenerator addCapacityDistribution(IScope scope, GamaPopGenerator gen, String featureName) {
		gen.setSpatialDistribution(SpatialDistribution.CAPACITY);
		gen.setSpatialDistributionFeature(featureName);
		return gen;
	}
	
	@operator(value = "add_density_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of density type with a constant "
			+ "number of maximum synthetic entities per square meter in each spatial entity",
		examples = @example(value = "my_pop_generator add_density_distribution 0.4", test = false))
	@no_test
	public static GamaPopGenerator addDensityDistribution(IScope scope, GamaPopGenerator gen, double density) {
		gen.setSpatialDistribution(SpatialDistribution.DENSITY);
		gen.setSpatialDistributionDensity(density);
		return gen;
	}
	
	@operator(value = "add_density_distribution", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Define the spatial distribution of density type. "
			+ "The specific density is given in each spatial entity by a specified feature",
		examples = @example(value = "my_pop_generator add_density_distribution \"density of inhabitants\"", test = false))
	@no_test
	public static GamaPopGenerator addDensityDistribution(IScope scope, GamaPopGenerator gen, String featureName) {
		gen.setSpatialDistribution(SpatialDistribution.DENSITY);
		gen.setSpatialDistributionFeature(featureName);
		return gen;
	}
	
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
		gen.setPathNestedGeometries(stringPathToGeometriesShapefile);
		
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
