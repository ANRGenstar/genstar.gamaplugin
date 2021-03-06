package genstar.gamaplugin.operators;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import core.configuration.dictionary.AttributeDictionary;
import core.configuration.dictionary.IGenstarDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.util.excpetion.GSIllegalRangedData;
import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.utils.GenStarGamaUtils;
import gospl.io.ipums.ReadIPUMSDictionaryUtils;
import msi.gama.common.util.FileUtils;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.no_test;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaMap;
import msi.gama.util.IList;
import msi.gaml.types.IType;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GenstarAdderOperators {
	
	@operator(value = "add_census_file", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add a census data file defined by its path (string), its type (\"ContingencyTable\", \"GlobalFrequencyTable\", \"LocalFrequencyTable\" or  \"Sample\"), its separator (string), the index of the first row of data (int) and the index of the first column of data (int) to a population_generator",
	examples = @example(value = "add_census_file(pop_gen, \"../data/Age_Couple.csv\", \"ContingencyTable\", \";\", 1, 1)", test = false))
	@no_test
	public static GamaPopGenerator addCensusFile(IScope scope, GamaPopGenerator gen, String path, String type, 
			String csvSeparator, int firstRowIndex, int firstColumnIndex) throws GamaRuntimeException {
		Path completePath = Paths.get(FileUtils.constructAbsoluteFilePath(scope, path, false));
		gen.getInputFiles().add(new GSSurveyWrapper(completePath, GenStarGamaUtils.toSurveyType(type), 
				csvSeparator.isEmpty() ? ',':csvSeparator.charAt(0), firstRowIndex, firstColumnIndex));
		return gen;
	}
	
	@operator(value = "add_ipums_micro_data", category = { "Gen*" }, concept = { "Gen*" })
	@doc(value = "add a micro-sample from IPUMS data base as a csv file with its dictionary",
			examples = @example(value = "add_marginals(pop_gen, ipums_micro_data_file, ipums_dictionary)"))
	@no_test
	public static GamaPopGenerator addIPUMSMicroData(IScope scope, GamaPopGenerator gen, String IPUMSFilePath, String IPUMSDictionaryFilePath) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		Path ipumsData_filePath = Paths.get(FileUtils.constructAbsoluteFilePath(scope, IPUMSFilePath, false));
		Path ipumsDictionary_filePath = Paths.get(FileUtils.constructAbsoluteFilePath(scope, IPUMSDictionaryFilePath, false));
		gen.getInputFiles().add(new GSSurveyWrapper(ipumsData_filePath, GSSurveyType.Sample,',',1,1));
		
		// Dictionaries from ipums
		Set<IGenstarDictionary<Attribute<? extends IValue>>> dds = null;
		try {
			dds = new ReadIPUMSDictionaryUtils().readDictionariesFromIPUMSDescription(ipumsDictionary_filePath.toFile());
		} catch (GSIllegalRangedData | NullPointerException e) {
			GamaRuntimeException.create(e, scope);
		}
		
		if (dds == null) { throw new NullPointerException("Genstar dictionary has not been load correctly"); }
		List<IGenstarDictionary<Attribute<? extends IValue>>> dicos = dds.stream()
				.sorted((d1,d2) -> d1.getLevel() < d2.getLevel() ? -1 : 1)
				.collect(Collectors.toList());
		gen.getInputAttributes().addAttributes(dicos.get(0).getAttributes());
		gen.setHouseholdAttributes((AttributeDictionary)dicos.get(1));
		
		return gen;
	}
	
	@operator(value = "add_mapper", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add a mapper between source of data for a attribute to a population_generator. "
			+ "A mapper is defined by the name of the attribute, the datatype of attribute (type), "
			+ "the corresponding value (map<list,list>) and the type of attribute (\"unique\" or \"range\")",
		examples = @example(value = " add_mapper(pop_gen, \"Age\", int, [[\"0 to 18\"]::[\"1 to 10\",\"11 to 18\"], "
				+ "[\"18 to 100\"]::[\"18 to 50\",\"51 to 100\"] , \"range\");", test = false))
	@no_test
	public static GamaPopGenerator addMapper(IScope scope, GamaPopGenerator gen, String referentAttributeName, IType dataType, GamaMap values ) {
		return addMapper(scope, gen,referentAttributeName, dataType, values, false);
	}
	

	// TODO : remove the type ... 
	@operator(value = "add_mapper", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add a mapper between source of data for a attribute to a population_generator. "
			+ "A mapper is defined by the name of the attribute, the datatype of attribute (type), "
			+ "the corresponding value (map<list,list>) and the type of attribute (\"unique\" or \"range\")",
	examples = @example(value = " add_mapper(pop_gen, \"Age\", int, [[\"0 to 18\"]::[\"1 to 10\",\"11 to 18\"], "
			+ "[\"18 to 100\"]::[\"18 to 50\",\"51 to 100\"] , \"range\");", test = false))
	@no_test
	public static GamaPopGenerator addMapper(IScope scope, GamaPopGenerator gen, String referentAttributeName, IType dataType, GamaMap values, Boolean ordered) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		if (referentAttributeName == null) return gen;
		
		AttributeFactory attf = AttributeFactory.getFactory();		

		Attribute<? extends IValue> referentAttribute = gen.getInputAttributes().getAttribute(referentAttributeName);
		
		if(referentAttribute != null) {	

			try {	
				String name = referentAttribute.getAttributeName() + "_" + (gen.getInputAttributes().getAttributes().size() + 1);
				gen.getInputAttributes().addAttributes(attf.createSTSMappedAttribute(name, GenStarGamaUtils.toDataType(dataType, ordered), referentAttribute, values));

				// We lose an information in the case it is an aggregatre 
				// Si keys ont 1 seules element -> aggregation
				
			//	dd.addAttributes(attf.createRangeAggregatedAttribute("Age_2", new GSDataParser()
			//			.getRangeTemplate(mapperA1.keySet().stream().collect(Collectors.toList())),
			//			referentAgeAttribute, mapperA1));
				
			} catch (GSIllegalRangedData e) {
				throw GamaRuntimeException.error("Wrong type in the record." + e.getMessage(), scope);
			}				
		}
		return gen;
	}
	

	
	@operator(value = "add_attribute", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) to a population_generator",
			examples = @example(value = "add_attribute(pop_gen, \"Sex\", string,[\"Man\", \"Woman\"])", test = false))
	@no_test
	public static GamaPopGenerator addAttribute(IScope scope, GamaPopGenerator gen, String name, IType dataType, IList value) {
		return addAttribute(scope, gen, name, dataType, value, false);
	}
	
	@operator(value = "add_range_attribute", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) to a population_generator",
			examples = @example(value = "add_attribute(pop_gen, \"Sex\", string,[\"Man\", \"Woman\"])", test = false))
	@no_test
	public static GamaPopGenerator addAttribute(IScope scope, GamaPopGenerator gen, String name, IList ranges, int lowest, int highest) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		Attribute<? extends IValue> newAttribute = null;
		try {
			newAttribute = gen.getAttf().createRangeAttribute(name, ranges, lowest, highest);
		} catch (GSIllegalRangedData | NullPointerException e) {
			GamaRuntimeException.create(e, scope);
		}
		gen.getInputAttributes().addAttributes(newAttribute);
		return gen;
	}

	@operator(value = "add_attribute", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) "
			+ "and attributeType name (type of the attribute among \"range\" and \"unique\") to a population_generator", 
			examples = @example(value = "add_attribute(pop_gen, \"iris\", string, liste_iris, \"unique\")", test = false))
	@no_test
	public static GamaPopGenerator addAttribute(IScope scope, GamaPopGenerator gen, String name, IType dataType, 
			IList value, String record, IType recordType) {
		return addAttribute(scope, gen, name, dataType, value, false, record, recordType);
	}	

	@operator(value = "add_attribute", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) to a population_generator",
			examples = @example(value = "add_attribute(pop_gen, \"Sex\", string,[\"Man\", \"Woman\"])", test = false))
	@no_test
	public static GamaPopGenerator addAttribute(IScope scope, GamaPopGenerator gen, String name, IType dataType, 
			IList value, Boolean ordered, String record, IType recordType) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		
		GamaPopGenerator genPop = addAttribute(scope, gen, name, dataType, value, ordered);
		genPop.getInputAttributes().addRecords();
		try {
			genPop.getInputAttributes().addRecords(
				gen.getAttf().createRecordAttribute(
					record, 
					GenStarGamaUtils.toDataType(recordType,false)/*GSEnumDataType.Integer*/, 
					genPop.getInputAttributes().getAttribute(name)
				)
			);
		} catch (GSIllegalRangedData e) {
			throw GamaRuntimeException.error("Wrong type for the record. " + e.getMessage(), scope);
		}
	
		return genPop;
		
	}	
	
	@operator(value = "add_attribute", can_be_const = true, category = { "Gen*" }, concept = { "Gen*"})
	@doc(value = "add an attribute defined by its name (string), its datatype (type), its list of values (list) and "
			+ "record name (name of the attribute to record) to a population_generator", 
			examples = @example(value = "add_attribute(pop_gen, \"iris\", string,liste_iris, \"unique\", \"P13_POP\")", test = false))
	@no_test
	public static GamaPopGenerator addAttribute(IScope scope, GamaPopGenerator gen, String name, IType dataType, IList value, Boolean ordered) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		
		try {
			Attribute<? extends IValue> newAttribute = 
					gen.getAttf().createAttribute(name, GenStarGamaUtils.toDataType(dataType,ordered), value);
			
			gen.getInputAttributes().addAttributes(newAttribute);
			 
			 // TODO : à revoir les records ..........
		//	 if (record != null && ! record.isEmpty()) {
		//		 gen.getInputAttributes().addRecords()
		//				 gen.getAttf()
		//				 .createIntegerRecordAttribute(record,newAttribute,Collections.emptyMap()));
			
			/*Attribute<? extends IValue> attIris = gen.getAttf()
					.createAttribute(name, toDataType(dataType,ordered), value);
			 gen.getInputAttributes().addAttributes(attIris);
			 if (record != null && ! record.isEmpty()) {
				 gen.getRecordAttributes().addAttributes(gen.getAttf()
						 .createRecordAttribute("population", GSEnumDataType.Integer,
								 attIris, Collections.emptyMap()));*/
		//	 }
		} catch (GSIllegalRangedData e) {
			throw GamaRuntimeException.error("Wrong type in the record." + e.getMessage(), scope);
		}
		return gen;
	}
	
	@operator(value = "add_marginals", category = { "Gen*" }, concept = { "Gen*" })
	@doc(value = "add a list of marginals (name of the attributes) to fit the population with, in any CO based algorithm",
			examples = @example(value = "add_marginals(pop_gen, [\"gender\",\"age\"])"))
	@no_test
	public static GamaPopGenerator addMarginals(IScope scope, GamaPopGenerator gen, IList names) {
		if (gen == null) { gen = new GamaPopGenerator(); }
		gen.setMarginals(gen.getInputAttributes().getAttributes().stream()
				.filter(att -> names.contains(att.getAttributeName()))
				.collect(Collectors.toList()));
		return gen;
	}
	
}