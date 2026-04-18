package org.nasdanika.models.genmodel.ecore;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.ecore.graph.processors.EcoreNodeProcessorFactoryCapabilityFactory.TargetRequirement;

public class ECoreGenGenModelProcessorsCapabilityFactory implements CapabilityFactory<TargetRequirement, Object> {

	@Override
	public boolean canHandle(Object requirement) {
		return requirement instanceof TargetRequirement;
	}

	@Override
	public CompletionStage<Iterable<CapabilityProvider<Object>>> create(
			TargetRequirement requirement,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		return CompletableFuture.completedStage(
				Collections.singleton(
						CapabilityProvider.of(
								new EcoreGenGenModelProcessorsFactory(requirement.requirement().context()))));
	}

}
