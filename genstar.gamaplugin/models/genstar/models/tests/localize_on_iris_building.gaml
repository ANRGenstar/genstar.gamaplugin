/**
* Name: localizeiris
* Author: ben
* Description: 
* Tags: Tag1, Tag2, TagN
*/

model localizeiris

global {
	file f_IRIS <- file("../../data/Rouen_iris.csv");
	file sample_iris <- file("../../data/Rouen_sample_iris.csv");
	file iris_shp <- file("../../data/shp/Rouen_iris_number.shp");

	// String constants
	file buildings_shp <- file("../../data/shp/buildings.shp");
	file capacity_buildings_shp <- file("../../data/shp/buildings_capacity.shp");

	//name of the property that contains the id of the census spatial areas in the shapefile
	string stringOfCensusIdInShapefile <- "CODE_IRIS";

	//name of the property that contains the id of the census spatial areas in the csv file (and population)
	string stringOfCensusIdInCSVfile <- "iris";

	//path to the file that will be used as support for the spatial regression (bring additional spatial data)
//	string stringPathToLandUseGrid <- "../../data/raster/occsol_rouen.tif";

	bool sample_based parameter:true init:false;
	bool capacity_building parameter:true init:true;

	geometry shape <- envelope(buildings_shp);

	init {		
		create building from: capacity_building ? capacity_buildings_shp : buildings_shp with:[capacity::int(get("capacity"))];	
		
		create iris_agent from: iris_shp with: [code_iris::string(read('CODE_IRIS'))];			
		
		gen_population_generator pop_gen;
		
		if sample_based {
			write "CO uniform sampling algorithm";
			pop_gen <- pop_gen with_generation_algo "Uniform Sampling";
			pop_gen <- add_census_file(pop_gen, sample_iris.path, "Sample", ",", 1, 0);
		}  else {
			write "SR direct sampling algorithm";
			pop_gen <- pop_gen with_generation_algo "Direct Sampling"; 
			pop_gen <- add_census_file(pop_gen, f_IRIS.path, "ContingencyTable", ",", 1, 1);
		}			
			
		// -------------------------
		// Setup "IRIS" attribute: INDIVIDUAL
		// -------------------------

		list<string> liste_iris <- [
			"765400602", "765400104","765400306","765400201",
			"765400601","765400901","765400302","765400604","765400304",
			"765400305","765400801","765400301","765401004","765401003",
			"765400402","765400603","765400303","765400103","765400504",
			"765401006","765400702","765400401","765400202","765400802",
			"765400502","765400106","765400701","765401005","765400204",
			"765401001","765400405","765400501","765400102","765400503",
			"765400404","765400105","765401002","765400902","765400403",
			"765400203","765400101","765400205"];
		
		pop_gen <- pop_gen add_attribute("iris", string, liste_iris, (sample_based?"iris":"P13_POP"), int);  
		
		// -------------------------
		// Spatialization 
		// -------------------------
		
		
		int max_capacity <- 1;
		if capacity_building { 
			pop_gen <- pop_gen localize_on_geometries(capacity_buildings_shp.path);
			pop_gen <- pop_gen add_capacity_constraint(max_capacity,max_capacity,0,2);
		} else {
			pop_gen <- pop_gen localize_on_geometries(buildings_shp.path);
		}
		
		pop_gen <- pop_gen localize_on_census(iris_shp.path);
		
		pop_gen <- pop_gen add_spatial_match(stringOfCensusIdInCSVfile,stringOfCensusIdInShapefile,1#km,200#m,1);

		// -------------------------			
		create people from: pop_gen;
		// -------------------------
		
		list<int> building_content <- building collect length(people overlapping each.shape);
		
		list<int> over_crowded <- building_content where (each > max_capacity);
		write "There is "+length(over_crowded)+" over crowded buildings: average = "+mean(over_crowded)+" | max = "+max(over_crowded);
		
		int well_sized_building <- building count (length(people overlapping each.shape) <= max_capacity);
		write "There is "+well_sized_building+" building with 1 people top inside";
	}
}

species people {
	string iris;
	rgb color <- first(iris_agent where(each.code_iris=iris)).color;

	aspect default { 
		draw circle(4) color: color border: #black;
	}
}

species iris_agent {
	string code_iris;
	rgb color <- rnd_color(255);
	
	aspect default {
		draw shape color:color  border: #black;
	}
}

species building {
	int capacity;
	aspect default {
		draw shape color:#lightgrey  border: #black;
	}
}

experiment Rouentemplate type: gui {
	output {
		display map type: opengl {
			species building;
			species iris_agent;
			species people;
		}
	}
}
