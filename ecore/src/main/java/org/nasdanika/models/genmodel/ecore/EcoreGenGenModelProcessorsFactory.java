package org.nasdanika.models.genmodel.ecore;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.nasdanika.common.Context;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Document;
import org.nasdanika.drawio.Geometry;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.Model;
import org.nasdanika.drawio.Node;
import org.nasdanika.drawio.Page;
import org.nasdanika.drawio.Root;
import org.nasdanika.drawio.style.Arrow;
import org.nasdanika.drawio.style.ConnectionStyle;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;
import org.nasdanika.models.ecore.graph.processors.EClassNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EClassifierNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EClassifierNodeProcessorFactory;
import org.nasdanika.models.ecore.graph.processors.EDataTypeNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EEnumNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EPackageNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EPackageNodeProcessorFactory;

@EPackageNodeProcessorFactory(nsURI = GenModelPackage.eNS_URI)
public class EcoreGenGenModelProcessorsFactory {

	private Context context;
		
	public EcoreGenGenModelProcessorsFactory(Context context) {
		this.context = context;
	}
	
	@EPackageNodeProcessorFactory(
			label = "Code Generation Model",
			actionPrototype = """
                    app-action:
                      content:
                        content-markdown:
                          source:
                            content-resource:
                              location: genmodel.md
					"""
	)
	public EPackageNodeProcessor createEPackageProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory, Object> config, 
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider,
			BiConsumer<Label, ProgressMonitor> labelConfigurator,
			ProgressMonitor progressMonitor) {		
		return new EPackageNodeProcessor(config, context, prototypeProvider) {
			
			@Override
			public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
				super.configureLabel(source, label, progressMonitor);
				if (labelConfigurator != null) {
					labelConfigurator.accept(label, progressMonitor);
				}
			}
			
			/**
			 * Generating a Drawio diagram
			 */
			@Override
			protected void generateDiagramAndGraphActions(Collection<Label> labels, ProgressMonitor progressMonitor) {
				super.generateDiagramAndGraphActions(labels, progressMonitor);
				
				try {
					Document document = Document.create(false, null);
					Page page = document.createPage();
					page.setName("GenModel");
					
					Model model = page.getModel();
					Root root = model.getRoot();
					Layer<?> backgroundLayer = root.getLayers().get(0);
					
					generateDrawioDiagram(
						ep -> backgroundLayer,	
						false, 
						false, 
						progressMonitor);
					
					org.nasdanika.drawio.Util.forceLayout(root, 1920, 1080);
												
					Files.writeString(new File("target/genmodel.drawio").toPath(), document.save(null));
				} catch (Exception e) {
					e.printStackTrace();
					throw new NasdanikaException(e);
				}								
			}			
			
		};
	}	
	
	@EClassifierNodeProcessorFactory
	public EClassifierNodeProcessor<?> createEClassifierProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory, Object> config, 
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider,
			BiConsumer<Label, ProgressMonitor> labelConfigurator,
			ProgressMonitor progressMonitor) {
		
		EObject eClassifier = ((EObjectNode) config.getElement()).get();
				
		if (eClassifier instanceof EClass) {
			return new EClassNodeProcessor(config, context, prototypeProvider) {
				
				@Override
				public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
					super.configureLabel(source, label, progressMonitor);
					if (labelConfigurator != null) {
						labelConfigurator.accept(label, progressMonitor);
					}
				}	
				
				@Override
				protected EList<? super Action> getMembersActionCollection(Action parent) {
					return parent.getChildren();
				}
				
				@Override
				protected EList<? super Action> getMembersCollection(Action membersAction) {
					return membersAction.getChildren();
				}
				
				@Override
				protected void addDiagramAction(Action action, Action diagramAction) {
					action.getSections().add(diagramAction);
				}
				
				@Override
				protected int getDiagramNodeWidth() {
					return Math.max(getTarget().getName().length() * 5, super.getDiagramNodeWidth());
				}
				
				@Override
				protected void createDrawioConnection(
						URI base, 
						Layer<?> layer, 
						EClassifierNodeProcessor<?> dependency,
						Node diagramNode, Node targetNode) {
					
					EClassifier targetEClassifier = dependency.getTarget();
					
					// Supertype
					if (getTarget().getESuperTypes().contains(targetEClassifier)) {
						// TODO - connect top center of the sub-class to the bottom center of super-class
						Connection inheritance = layer.createConnection(
								diagramNode.createConnectionPoint(0.5, 0), 
								targetNode.createConnectionPoint(0.5, 1));
						ConnectionStyle style = inheritance.getStyle();
						style
							.rounded(false)
							.endArrow(Arrow.BLOCK);
							
						style.put("edgeStyle", "orthogonalEdgeStyle");
						style.put("orthogonalLoop", "1");
						style.put("jettySize", "auto");
						style.put("html", "1");
						style.put("endFill", "0");
					}
					
					// Reference
					for (EReference ref: getTarget().getEReferences()) {
						if (ref.getEType() == targetEClassifier && shallCreateConnection(ref)) {
							Connection refConnection = layer.createConnection(diagramNode, targetNode);
							ConnectionStyle style = refConnection.getStyle();
							style.rounded(false);
							
							style.put("orthogonalLoop", "1");
							style.put("jettySize", "1");
							style.put("html", "1");
							if (ref.isMany()) {
								style.startArrow(Arrow.DIAMOND_THIN);
								style.startFill(ref.isContainment());
							}
							
							WidgetFactory refWidgetFactory = eReferenceWidgetFactories.get(ref.getName());
							if (refWidgetFactory != null) {
								Object refLink = refWidgetFactory.createLink(base, progressMonitor);
								if (refLink instanceof org.nasdanika.models.app.Link) {
									refConnection.setLink(((org.nasdanika.models.app.Link) refLink).getLocation());
								}								
							}
							
							EReference opposite = ref.getEOpposite();
							if (opposite == null) {
								refConnection.setLabel(ref.getName());								
							} else {								
								if (opposite != null && opposite.isMany()) {
									style.endArrow(Arrow.DIAMOND_THIN);
									style.endFill(ref.isContainment());
								}
								
								Node startLabel = refConnection.createNode();
								Map<String, String> edgeLabelStyle = org.nasdanika.drawio.Util.loadStyle("edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[]");
								startLabel.getStyle().putAll(edgeLabelStyle);
								
								startLabel.setLabel(ref.getName());
								Geometry slg = startLabel.getGeometry();
								slg.setX(0.7);
								slg.setRelative(true);
//								
								Node endLabel = refConnection.createNode();
								endLabel.getStyle().putAll(edgeLabelStyle);
								endLabel.setLabel(opposite.getName());
								Geometry elg = endLabel.getGeometry();
								elg.setX(0.3);
								elg.setRelative(true);
							}
						}
					}
				}
				
			};
		}
		
		if (eClassifier instanceof EEnum) {
			return new EEnumNodeProcessor(config, context, prototypeProvider);
		}
		
		return new EDataTypeNodeProcessor<EDataType>(config, context, prototypeProvider);		
	}
	
	protected boolean shallCreateConnection(EReference ref) {
		EReference opposite = ref.getEOpposite();
		if (opposite == null) {
			return true;
		}
		
		return refUri(ref).compareTo(refUri(opposite)) < 0 ;
		}

	protected String refUri(EReference ref) {
		return ref.getEContainingClass().getEPackage().getNsURI() + "#" + ref.getEContainingClass().getName() + "." + ref.getName();
	} 

}
