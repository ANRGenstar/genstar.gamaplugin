/**
* Name: DomizGenstar
* Based on the internal empty template. 
* Author: kevinchapuis
* Tags: 
*/


model DomizGenstar

global {
	
	shape_file buildings0_shape_file <- shape_file("data/buildings.shp");
	geometry shape <- envelope(buildings0_shape_file);

	csv_file age_sex_domiz0_csv_file <- csv_file("data/age_sex_domiz.csv");
	string domiz_population <- "outputs/domiz_population.csv";

	list<string> age <- ["0-4", "5-11", "12-17", "18-59", "60+"];
	list<string> sex <- ["Female","Male"];
	
	init {
		
		// Create the building you want to locate agent within
		create building from:buildings0_shape_file;
		
		// Setup the population generator
		gen_population_generator pop_gen;
		pop_gen <- pop_gen with_generation_algo "Direct sampling";  //"Sample";//"IS";
		
		// Define input demographic data file(s)
		pop_gen <- add_census_file(pop_gen, age_sex_domiz0_csv_file.path, "ContingencyTable", ",", 1, 1);
		
		// Add the desired attribute to the population
		pop_gen <- pop_gen add_range_attribute("age", age, 0, 100);
		pop_gen <- pop_gen add_attribute("sex", string, sex);
		
		pop_gen <- pop_gen localize_on_geometries(buildings0_shape_file.path);
		
		create people from: pop_gen;
		
		save people to:domiz_population type:csv;
	}

}

species people {
	int age;
	string sex;
	aspect default { draw circle(1) color:#brown;}
}

species building { aspect default {draw shape.contour color:#black;}}

experiment xp {
	output main { display "main" { species building; species people; }}
}
