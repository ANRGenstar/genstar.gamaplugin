package genstar.gamaplugin.operators;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import core.metamodel.entity.ADemoEntity;
import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.utils.GenStarConstant;
import genstar.gamaplugin.utils.GenStarConstant.NetworkEngine;
import genstar.gamaplugin.utils.GenStarGamaUtils;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.no_test;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.graph.IGraph;
import msi.gaml.species.GamlSpecies;
import spin.SpinNetwork;
import spin.algo.factory.SpinNetworkFactory;

public class GenstarNetworkOperators {
	
	@operator(value = "add_network", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Configure a new graph generator with a generation engine and a given beta probability", 
		examples = @example(value = "add_network(my_pop_generator, 'mygraph', 'random', 0.1)", test = false))
	@no_test
	public static GamaPopGenerator addGraphGenerator(IScope scope, GamaPopGenerator gen, String graphName, String graphGenerator, Double beta) {
		testNetworkEngine(graphGenerator, scope);
		gen.addNetworkGenerator(graphName, SpinNetworkFactory.getInstance().getSpinPopulationGenerator(graphName, graphGenerator, beta, 0));
		return gen;
	}
	
	@operator(value = "add_network", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Configure a new graph generator with a generation engine and a given k mode for neighbors number", 
		examples = @example(value = "add_network(my_pop_generator, 'mygraph', 'random', 0.1)", test = false))
	@no_test
	public static GamaPopGenerator addGraphGenerator(IScope scope, GamaPopGenerator gen, String graphName, String graphGenerator, Integer k) {
		testNetworkEngine(graphGenerator, scope);
		gen.addNetworkGenerator(graphName, SpinNetworkFactory.getInstance().getSpinPopulationGenerator(graphName, graphGenerator, 0.0, k));
		return gen;
	}

	@operator(value = "add_network", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Configure a new graph generator with a generation engine, a given beta probability and a k mode for neighbors number", 
		examples = @example(value = "add_network(my_pop_generator, 'mygraph', 'random', 0.1)", test = false))
	@no_test
	public static GamaPopGenerator addGraphGenerator(IScope scope, GamaPopGenerator gen, String graphName, String graphGenerator, Double beta, Integer k) {
		testNetworkEngine(graphGenerator, scope);
		gen.addNetworkGenerator(graphName, SpinNetworkFactory.getInstance().getSpinPopulationGenerator(graphName, graphGenerator, beta, k));
		return gen;
	}
	
	@operator(value = "add_network", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Configure a new graph generator with a generation engine", 
		examples = @example(value = "add_network(my_pop_generator, 'mygraph', 'random')", test = false))
	@no_test
	public static GamaPopGenerator addGraphGenerator(IScope scope, GamaPopGenerator gen, String graphName, String graphGenerator) {
		testNetworkEngine(graphGenerator, scope);
		gen.addNetworkGenerator(graphName, SpinNetworkFactory.getInstance().getSpinPopulationGenerator(graphName, graphGenerator, 0.0, 0));
		return gen;
	}
	
	@operator(value = "get_network", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Construct and return a network build by the generator", 
		examples = @example(value = "get_network(my_pop_generator, 'mygraph')", test = false))
	@no_test
	public static IGraph getGraph(IScope scope, GamaPopGenerator gen, String networkName) {
		SpinNetwork net = gen.getNetwork(networkName);		
		return GenStarGamaUtils.toGAMAGraph(scope, net, gen);
	}
	
	@operator(value = "associate_population_agents", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "Associate a population of agent to a given generator", 
		examples = @example(value = "associate_population_agents(my_pop_generator, my_species_of_agent)", test = false))
	@no_test
	public static GamaPopGenerator associatePopulation(IScope scope, GamaPopGenerator gen, GamlSpecies pop) {
		Object[] entity = gen.getGeneratedPopulation().toArray();
		
		for(int i = 0 ; i < pop.getPopulation(scope).length(scope) ; i ++) {			
			IAgent agt = pop.getPopulation(scope).getAgent(i);
			gen.addAgent( (ADemoEntity) entity[i], agt);
		}
		return gen;
	}	
	
	/*
	 * Private test for network generator aliases
	 */
	private static void testNetworkEngine(String engine, IScope scope) {
		if (Stream.of(GenStarConstant.NetworkEngine.values()).noneMatch(e -> e.getMatch(engine))) {
			throw GamaRuntimeException.error("The network engine "+engine+" is not supported ["+
					Stream.of(GenStarConstant.NetworkEngine.values()).map(NetworkEngine::getDefault).collect(Collectors.joining("; "))+"]", scope);
		}
	}
}
