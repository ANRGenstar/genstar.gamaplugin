package genstar.gamaplugin.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.value.IValue;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;
import msi.gama.common.interfaces.IKeyword;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.runtime.IScope;
import msi.gama.util.IContainer;
import msi.gaml.descriptions.SpeciesDescription;
import msi.gaml.descriptions.VariableDescription;
import msi.gaml.types.IType;
import msi.gaml.types.Types;

/**
 * The main object to manage translation between Gama and Genstar objects
 * 
 * @author kevinchapuis
 *
 */
public class GenStarGamaConverter {
	
	/**
	 * 
	 * @param attribute
	 * @return
	 */
	public static Attribute<? extends IValue> conertAttributeFromGamlToGenstar(Object attribute) {
		Attribute<? extends IValue> att = null;
		
		
		return att;
	}
	
	/**
	 * 
	 * @param scope
	 * @param agents
	 * @return
	 * @throws GSIllegalRangedData
	 */
	public static Set<Attribute<? extends IValue>> convertAttributesFromGamlToGenstar(
			IScope scope, IContainer<?, ? extends IAgent> agents) throws GSIllegalRangedData {
		
		Set<Attribute<? extends IValue>> mySet = new HashSet<>();
		final AttributeFactory gaf = AttributeFactory.getFactory();
		
		final Set<String> NON_SAVEABLE_ATTRIBUTE_NAMES = new HashSet<>(Arrays.asList(IKeyword.PEERS,
				IKeyword.LOCATION, IKeyword.HOST, IKeyword.AGENTS, IKeyword.MEMBERS, IKeyword.SHAPE));
		final SpeciesDescription species =
				agents instanceof msi.gama.metamodel.population.IPopulation ? 
						((msi.gama.metamodel.population.IPopulation) agents).getSpecies().getDescription()
						: agents.getGamlType().getContentType().getSpecies();
		
		for (VariableDescription vd : species.getAttributes()) {
			if (NON_SAVEABLE_ATTRIBUTE_NAMES.contains(vd.getName())) { continue; }
			Attribute<? extends IValue> att = gaf.createAttribute(vd.getName(), getType(vd.getGamlType()), 
					agents.stream(scope).map(a -> a.getDirectVarValue(scope, vd.getName()).toString()).toList());
			mySet.add(att);
			
		}
		
		return mySet;
	}
	
	/**
	 * 
	 * @param agents
	 * @return
	 */
	public static SimpleFeatureType extractFeatureType(IScope scope, IContainer<?, ? extends IAgent> agents) {
		SimpleFeatureType type = null;
		
		return type;
	}
	
	public static GSEnumDataType getType(IType gamaType) {
		if (gamaType == Types.INT) { return GSEnumDataType.Integer; }
		else if (gamaType == Types.FLOAT) {return GSEnumDataType.Continue;}
		else if (gamaType == Types.BOOL) {return GSEnumDataType.Boolean;}
		return GSEnumDataType.Nominal;
	}
	
}
