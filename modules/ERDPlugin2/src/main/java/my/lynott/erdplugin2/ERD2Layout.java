package my.lynott.erdplugin2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import org.graffiti.plugins.algorithms.sugiyama.SugiyamaAlgorithm;
import org.graffiti.plugins.algorithms.sugiyama.crossmin.Median;
import org.graffiti.plugins.algorithms.sugiyama.decycling.DFSDecycling;
import org.graffiti.plugins.algorithms.sugiyama.erd.ERDTweeny;
import org.graffiti.plugins.algorithms.sugiyama.erd.GE2Tweeny;
import org.graffiti.plugins.algorithms.sugiyama.erd.Tweeny2GR;
import org.graffiti.plugins.algorithms.sugiyama.layout.SocialBrandesKoepf;
import org.graffiti.plugins.algorithms.sugiyama.levelling.LongestPath;
import org.graffiti.plugins.algorithms.sugiyama.util.SugiyamaData;
import org.jfree.util.Log;
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

	/**
	 * The set of Sugiyama algorithms to be executed.
	 */
	SugiyamaAlgorithm[] algorithms=null;
	
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
	
	/** An instance of the nbm logger */
	final java.util.logging.Logger LOG;
	
	/** This class name -- for use by logging invocation */
	String name="ERD2Layout";

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
		
		/* == ENABLE LOGGING == */
		LOG = java.util.logging.Logger.getLogger(this.getClass().getName());
	}

	//	@Override
	public void initAlgo() {
		executing = true;
	}

	//	@Override
	public void setGraphModel(GraphModel gm) {
		this.graphModel = gm;
	}


	//	@Override
	@SuppressWarnings("null")
	public void goAlgo() {
		Log.info("Entering ERD2Layout goAlgo");
		/*
		 * Retrieve the graph from the Gephi infrastructure.
		 */
		Graph g = graphModel.getGraphVisible();

		/*
		 * Lock the graph so no other part of  Gephi can access it.
		 */
		g.readLock();
		
		/*
		 * Create an instance of the conversion program GE2Tweeny
		 * passing it the (existing) Gephi graph.
		 */
		ge2t = new GE2Tweeny(g);

		/*
		 * Convert the graph into an instance of ERDTweeny
		 */		
		
		LOG.info("Beginning conversion to Tweeny");
		erdt = ge2t.convertToT();
		LOG.info("Returned from conversion to Tweeny");
		
		/*
		 * Create the SugiyamaData object, and populate it with
		 * the algorithms it will run on the subject graph.
		 */
		SugiyamaData sd = new SugiyamaData();
		buildAlgorithms();


		/*
		 * Create a new instance of Tweeny-to-Gravisto.
		 * (This call will change based on the source of the backend algorithms
		 * employed. TODO: Find the Java Pattern that meets this need.)
		 */
		t2gr = new Tweeny2GR(erdt,sd);
		

		/*
		 * Convert the graph from ERDTweeny to Gravisto.

		 * (This call will also change based on the source of the backend algorithms.)
		 */		
		t2gr.convertToGR();

		/* 
		 * Initiate the backend algorithm set: its gateway is the Sugiyama class. 
		 *
		 */
		sg = new Sugiyama(sd);

		/*
		 * Execute the backend Sugiyama algorithm set.
		 * 
		 */
		sg.execute();

		/*
		 * At this point, the backend algorithms have been executed.
		 *
		 * Instantiate a new Gravisto-to-Tweeny class, passing it the
		 * graph from SugiyamaData, and the ERDTweeny class.
		 * Perform the conversion.
		 */
		gr2t = new GR2Tweeny((FastGraph) sd.getGraph(), erdt);
		gr2t.convertToT();

		/*
		 * Instantiate a new Tweeny-to-Gephi class passing it
		 * ERDTweeny and the Gephi graph to be updated.
		 * Perform the conversion.
		 */

		t2ge = new Tweeny2GE(erdt,g);
		t2ge.convert2GE();

		/* 
		 * Now the Gephi graph has been updated with the results
		 * from the backend algorithms.
		 */

		/*
		 * Unlock the graph -- the counterpart to the earlier readLock.
		 */

		g.readUnlock();
		
		Log.info("Leaving ERD2Laout goAlgo");

	}

	//	@Override
	public boolean canAlgo() {
		return false;
	}

	//	@Override
	public void endAlgo() {		
		/* 
		 * Set the boolean var executing to false, signaling to the infrastructure 
		 * that the algorithm has halted and the graph can be read.
		 */
		
		executing = false;
	}

	//	@Override
	public LayoutProperty[] getProperties() {
		/* 
		 * This method failed with a message about not being able
		 * to find the size of something properties-related 
		 * (specified by an internal variable.)
		 * 
		 * Made two changes: provided the size of the new ArryList (2);
		 * changed the format of properties.add to the alternate
		 * format, where the index is provided as the first
		 * parameter. 
		 */
        LayoutProperty[] properties = new LayoutProperty[2];
        final String ERD2LAYOUT = "ERD2Layout";

        try {
            properties[0] = LayoutProperty.createProperty(
                    this, Integer.class,
                    "Area size",
                    ERD2LAYOUT,
                    "The area size",
                    "getAreaSize", "setAreaSize");
            properties[1] = LayoutProperty.createProperty(
                    this, Float.class,
                    "Speed",
                    ERD2LAYOUT,
                    "The speed at which the nodes move",
                    "getSpeed", "setSpeed");
           
        } catch (Exception e) {
            e.printStackTrace();
        }
		return properties;
	}

	//	@Override
	public void resetPropertiesValues() {
		areaSize = 1000;
		speed = 1f;
	}

	public Float getSpeed() {
		return speed;
	}

	public void setSpeed(Float speed) {
		this.speed = speed;
	}

	public Integer getAreaSize() {
		return areaSize;
	}

	public void setAreaSize(Integer area) {
		this.areaSize = area;
	}

	//	@Override
	public LayoutBuilder getBuilder() {
		LayoutBuilder lb = new ERD2LayoutBuilder();
		return lb;
	}
	
	/*
	 * Methods above this point are standard from gephi.
	 * Those below are added to support the operations above.
	 */
	
	/*
	 * Configure the algorithms to be executed by the Sugiyama code
	 */
	public void buildAlgorithms() {
		sd.setAlgorithmBinaryNames(getAlgorithms());
		
		sd.setLastSelectedAlgorithms(getAlgList());
		
		sd.setSelectedAlgorithms(getSugiyamaAlgorithms());
		
		sd.setAlgorithmMap(getAlgMap());
		
	}
	public ArrayList<String[]> getAlgorithms(){
		ArrayList<String[]> algs = new ArrayList<String[]>();
		String[] algArray= 
			{"C:\\Users\\mklnt\\ew\\eclipse-workspace22\\ERDPlugin1\\target\\classes\\org\\graffiti\\plugins\\algorithms\\sugiyama\\decycling\\DFSDecycling",
			"C:\\Users\\mklnt\\ew\\eclipse-workspace22\\ERDPlugin1\\target\\classes\\org\\graffiti\\plugins\\algorithms\\sugiyama\\levelling\\LongestPath", 
			"C:\\Users\\mklnt\\ew\\eclipse-workspace22\\ERDPlugin1\\target\\classes\\org\\graffiti\\plugins\\algorithms\\sugiyama\\crossmin\\Median",
			"C:\\Users\\mklnt\\ew\\eclipse-workspace22\\ERDPlugin1\\target\\classes\\org\\graffiti\\plugins\\algorithms\\sugiyama\\layout\\SocialBrandesKoepf"};
		algs.add(algArray);
		return algs;
	}

	public String[] getAlgList() {
		String[] algList = new String[4];
		algList[0] = "DFSDecycling";
		algList[1] = "LongestPath";
		algList[2] = "Median";
		algList[3] = "SocialBrandesKoepf";
		return algList;
	}
	
	@SuppressWarnings("null")
	public SugiyamaAlgorithm[] getSugiyamaAlgorithms() {
		

		
		algorithms[0] = new DFSDecycling();
		algorithms[1] = new LongestPath();
		algorithms[2] = new Median();
		algorithms[3] = new SocialBrandesKoepf();

		return  algorithms;
	}
	
	@SuppressWarnings("null")
	public HashMap<String, SugiyamaAlgorithm>  getAlgMap() {
		
		HashMap<String, SugiyamaAlgorithm> algMap=null;
		
		algMap.put("DecyclingAlgorithm",algorithms[0]);
		algMap.put("LevellingAlgorithm", algorithms[1]);
		algMap.put("CrossMinAlgorithm", algorithms[2]);
		algMap.put("LayoutAlgorithm",algorithms[3]);
		return algMap;
	}
	
}
