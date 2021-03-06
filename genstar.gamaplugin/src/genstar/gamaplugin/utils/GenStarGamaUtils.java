package genstar.gamaplugin.utils;

import java.util.Optional;

import org.graphstream.graph.Edge;

import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.metamodel.value.binary.BooleanValue;
import core.metamodel.value.numeric.ContinuousValue;
import core.metamodel.value.numeric.IntegerValue;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSEnumDataType;
import genstar.gamaplugin.types.GamaPopGenerator;
import genstar.gamaplugin.types.GamaRange;
import genstar.gamaplugin.types.GamaRangeType;
import genstar.gamaplugin.utils.GenStarConstant.GenerationAlgorithm;
import genstar.gamaplugin.utils.GenStarConstant.InputDataType;
import genstar.gamaplugin.utils.GenStarConstant.SpatialDistribution;
import gospl.algo.IGosplConcept;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.IShape;
import msi.gama.runtime.IScope;
import msi.gama.util.graph.GamaGraph;
import msi.gaml.types.IType;
import msi.gaml.types.Types;
import spin.SpinNetwork;

public class GenStarGamaUtils {
	
	/**
	 * The spatial distribution available in the Genstar API
	 * @param distribution
	 * @return
	 */
	public static SpatialDistribution toSpatialDistribution(String distribution) {
		if(SpatialDistribution.AREA.getMatch(distribution))
			return SpatialDistribution.AREA;
		if(SpatialDistribution.CAPACITY.getMatch(distribution))
			return SpatialDistribution.CAPACITY;
		if(SpatialDistribution.DENSITY.getMatch(distribution))
			return SpatialDistribution.DENSITY;
		return null;
	}
	
	/**
	 * Get Genstar API Survey type enum representation
	 * @param type
	 * @return
	 */
	public static GSSurveyType toSurveyType(String type) {
		if (InputDataType.CONTINGENCY.getMatch(type))
			return GSSurveyType.ContingencyTable;
		if (InputDataType.FREQUENCY.getMatch(type))
			return GSSurveyType.GlobalFrequencyTable;
		if (InputDataType.LOCAL.getMatch(type))
			return GSSurveyType.LocalFrequencyTable;
		if (InputDataType.SAMPLE.getMatch(type))
			return GSSurveyType.Sample;
		return null;
	}
	
	/**
	 * The generation algorithm given by the Genstar API
	 * @param algo
	 * @return
	 */
	public static IGosplConcept.EGosplAlgorithm toGosplAlgorithm(String algo){
		if (GenerationAlgorithm.DIRECTSAMPLING.getMatch(algo))
			return IGosplConcept.EGosplAlgorithm.DS;
		if (GenerationAlgorithm.HIERARCHICALSAMPLING.getMatch(algo))
			return IGosplConcept.EGosplAlgorithm.HS;
		if (GenerationAlgorithm.UNIFORMSAMPLING.getMatch(algo))
			return IGosplConcept.EGosplAlgorithm.US;
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	public static GSEnumDataType toDataType(final IType type, final boolean ordered) {
		int t = type.id();
		if (t == IType.FLOAT)
			return GSEnumDataType.Continue;
		if (t == IType.INT)
			return GSEnumDataType.Integer;
		if (t == IType.BOOL)
			return GSEnumDataType.Boolean;
		if (t == GamaRangeType.id  )
			return GSEnumDataType.Range;
		if (ordered)
			return GSEnumDataType.Order;
		return GSEnumDataType.Nominal; 
	}

	public static Object toGAMAValue(IScope scope, IValue val, boolean checkEmpty) {
		GSEnumDataType type= val.getType();
		if (checkEmpty && val.equals(val.getValueSpace().getEmptyValue())) return toGAMAValue(scope, val.getValueSpace().getEmptyValue(), false);
		if (type == GSEnumDataType.Boolean) {
			return ((BooleanValue) val).getActualValue();
		}
		if (type == GSEnumDataType.Continue) {
			if (val instanceof RangeValue) return toGAMARange(val);
			return ((ContinuousValue) val).getActualValue ();
		}
		if (type == GSEnumDataType.Integer) {
			if (val instanceof RangeValue) return toGAMARange(val);
			return ((IntegerValue) val).getActualValue();
		}
		if (type == GSEnumDataType.Range) {
			return toGAMARange(val);
		}
		return val.getStringValue();
	}
	
	static GamaRange toGAMARange(IValue val) {
		RangeValue rVal = (RangeValue) val;
		return new GamaRange(rVal.getBottomBound().doubleValue(), rVal.getTopBound().doubleValue());
	}

	@SuppressWarnings("rawtypes")
	public static Object toGAMAValue(IScope scope, IValue valueForAttribute, boolean checkEmpty, IType type) {
		Object gamaValue = toGAMAValue(scope, valueForAttribute, checkEmpty);
		if(type != null && gamaValue instanceof GamaRange) {
			return ((GamaRange) gamaValue).cast(scope, type);
		}
		return gamaValue;
	}
	
	
	public static GamaGraph<IAgent,IShape> toGAMAGraph(IScope scope, SpinNetwork net, GamaPopGenerator gen) {
		IType<?> nodeType ;
		
		Optional<IAgent> first = gen.getAgents().stream().findFirst();
		if (first.isPresent()) {
			nodeType = first.get().getGamlType();
		} else {
			return null;
		}
		
		GamaGraph<IAgent,IShape> gamaNetwork = new GamaGraph<>(scope, net.isDirected(),nodeType,Types.GEOMETRY);
		
		for(IAgent agt : gen.getAgents()) {
			gamaNetwork.addVertex(agt);
		}
		
		for(Edge e : net.getLinks()) {
			IAgent sourceAgt = gen.getAgent(net.getDemoEntityNode(e.getNode0()));
			IAgent targetAgt = gen.getAgent(net.getDemoEntityNode(e.getNode1()));
			
			gamaNetwork.addEdge(sourceAgt, targetAgt);
		}

		return gamaNetwork;
	}	
	
}
