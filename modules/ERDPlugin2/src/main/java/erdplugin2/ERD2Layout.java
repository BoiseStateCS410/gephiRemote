package erdplugin2;

import org.gephi.graph.api.EdgeIterable;



import org.gephi.graph.api.Graph;

import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.graffiti.graph.FastGraph;
import org.graffiti.plugins.algorithms.sugiyama.Sugiyama;
import org.graffiti.plugins.algorithms.sugiyama.erd.ERDTweeny;
import org.graffiti.plugins.algorithms.sugiyama.erd.GE2Tweeny;
import org.graffiti.plugins.algorithms.sugiyama.erd.Tweeny2GR;
import org.graffiti.plugins.algorithms.sugiyama.util.SugiyamaData;
import org.graffiti.plugins.algorithms.sugiyama.erd.GR2Tweeny;
import org.graffiti.plugins.algorithms.sugiyama.erd.Tweeny2GE;

/*
 * Derived from the Gephi Plugin environment's GridLayout by M. Bastian.

 *
 * Additional code by Michael Lynott
 */

/**
 * Layout builder for ERD2Layout: 
 * 
 * @author Michael Lynott
 */
@SuppressWarnings("unused")
public class ERD2Layout implements Layout {

	/** Signal the algorithm that Gephi requires that algorithm
	 *   to compact its returned graph to a size within this target area.
	 *   (I think.) Setting it to -1 negates any size check by the Gephi
	 *   Layout infrastructure.)
	 */
	private int areaSize = -1;

	/** A required instance of the Gephi Plugin LayoutBuilder class. */
	private final LayoutBuilder builder;

	/** An instance of the ERDTweeny class. */
	private ERDTweeny erdt;

	/** To show the Gephi Plugin manager process that the algorithm is executing.
	 *   It is set true in initAlgo, then set to false in endAlgo.
	 *   Otherwise it remains unchanged.
	 */
	private boolean executing = false;

	/** An instance of the Gephi Graph */
	private Graph g;

	/** An instance of the Gephi-to-ERDTweeny conversion program. */
	private GE2Tweeny ge2t;

	/** Required by the Gephi Layout process. */
	private GraphModel graphModel;

	/** An instance of the Gravisto-to-ERDTweeny conversion program. */
	private GR2Tweeny gr2t;

	/** An instance of Gravisto's SugiyamaData */
	private SugiyamaData sd;

	/** An instance of the backend algorithm, sugiyama class */
	Sugiyama sg;

	/** Control the transition speed for graph animation. 
	 *   This variable is ignored in the ERDLayout process,
	 *   but must be set within the Gephi Layout class.
	 */
	private float speed;

	/** An instance of the ERDTweeny-to-Gephi conversion program. */
	private Tweeny2GE t2ge;

	/** An instance of the ERDTweeny-to-Gravisto conversion program. */
	private Tweeny2GR t2gr;

	/*
	 * 
	 * MAIN
	 * 
	 */

	public ERD2Layout(ERD2LayoutBuilder builder) {
		this.builder = builder;
	}

	//	@Override
	public void initAlgo() {
		executing = true;

		Graph g = graphModel.getGraphVisible();
		g.readLock();

		/*
		 * Create an instance of the conversion program GE2Tweeny
		 * passing it the (existing) Gephi graph.
		 */

		ge2t = new GE2Tweeny(g);

		/*
		 * Convert the graph into an instance of ERDTweeny
		 */		

		erdt = ge2t.convertToT();

		/*
		 * Create a new instance of Tweeny2GR.
		 * (This call will change based on the source of the backend algorithms
		 * employed. TODO: Find the Java Pattern that meets this need.
		 */

		t2gr = new Tweeny2GR(erdt);

		/*
		 * Convert the graph from ERDTweeny to Gravisto.
		 * Store the graph in Gravisto's SugiyamaData
		 * as is customary in Gravisto.
		 * (This call will also change based on the source of the backend algorithms.)
		 */		
		SugiyamaData sd = t2gr.convertToGR();

	}

	//	@Override
	public void setGraphModel(GraphModel gm) {
		this.graphModel = gm;
	}


	//	@Override
	@SuppressWarnings("null")
	/*
	 * At this point, the provided graph has been converted into ERDTweeny,
	 * and into a graph for use by the backend algorithm.
	 */
	public void goAlgo() {
		/* 
		 * Initiate the backend algorithm: its gateway is the Sugiyama class. 
		 *
		 */
		sg = new Sugiyama(sd);

		/*
		 * Execute the backend Sugiyama algorithm set.
		 * ERDExecute is an added method in the Sugiyama code.
		 * which adds in the command to return
		 */
		sg.execute();

		/*
		 * At this point, the backend algorithms have been executed.
		 */

	}

	//	@Override
	public boolean canAlgo() {
		return false;
	}

	//	@Override
	public void endAlgo() {
		/*
		 * On entry, the backend algorithms have been executed.
		 * Now convert the backend graph to ERDTweeny, then use
		 * ERDTweeny to update the Gephi graph.
		 */
		
		/*
		 * Instantiate a new Gravisto-to-Tweeny class, passing it the
		 * graph from SugiyamaData, and the ERDTweeny class.
		 */
		gr2t = new GR2Tweeny((FastGraph) sd.getGraph(), erdt);
		gr2t.convertToT();

		/*
		 * Instantiate a new Tweeny-to-Gephi class passing it
		 * ERDTweeny and the Gephi graph to be updated.
		 */

		t2ge = new Tweeny2GE(erdt,g);
		t2ge.convert2GE();

		/* 
		 * Now the Gephi graph has been updated with the results
		 * from the backend algorithms.
		 */

		/*
		 * Unlock the graph -- the counterpart to the readLock in initAlgo.
		 * Set the boolean var executing to false, signaling to the infrastructure 
		 * that the algorithm has halted and the graph can be read.
		 */

		g.readUnlock();
		
		executing = false;
	}

	//	@Override
	public LayoutProperty[] getProperties() {
		return null;
	}

	//	@Override
	public void resetPropertiesValues() {
		areaSize = 1000;
		speed = 1f;
	}

	//	@Override
	public LayoutBuilder getBuilder() {
		LayoutBuilder lb = new ERD2LayoutBuilder();
		return lb;
	}
}


