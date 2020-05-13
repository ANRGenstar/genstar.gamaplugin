/**
* Name: VinhPhucGenstar
* Based on the internal empty template. 
* Author: kevinchapuis
* Tags: 
*/


model VinhPhucGenstar

global {
	
	string ipums_micro_data <- "data/fake_ipums_style_micro_data.csv";
	string ipums_dictionary <- "data/ipumsi_00004.cbk";

	init {
		
		gen_population_generator pop_gen;
		pop_gen <- pop_gen add_ipums_micro_data(ipums_micro_data, ipums_dictionary);
		
	}
	
}

