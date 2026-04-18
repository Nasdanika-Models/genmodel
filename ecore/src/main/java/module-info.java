import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.models.genmodel.ecore.ECoreGenGenModelProcessorsCapabilityFactory;

module org.nasdanika.models.genmodel.ecore {
		
	requires transitive org.eclipse.emf.codegen.ecore;
	requires transitive org.nasdanika.models.ecore.graph;
	requires org.eclipse.emf.common;
	
	exports org.nasdanika.models.genmodel.ecore;
	opens org.nasdanika.models.genmodel.ecore; // For loading resources

	provides CapabilityFactory with	ECoreGenGenModelProcessorsCapabilityFactory; 		
	
}
