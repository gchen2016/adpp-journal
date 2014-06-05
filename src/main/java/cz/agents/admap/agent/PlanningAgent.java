package cz.agents.admap.agent;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.jgrapht.util.HeuristicToGoal;

import tt.euclid2i.EvaluatedTrajectory;
import tt.euclid2i.Point;
import tt.euclid2i.probleminstance.Environment;
import tt.euclid2i.region.Circle;
import tt.euclidtime3i.L2Heuristic;
import tt.euclidtime3i.Region;
import tt.euclidtime3i.ShortestPathHeuristic;
import tt.euclidtime3i.region.MovingCircle;
import tt.euclidtime3i.region.MovingCircleMinusPoint;

/**
 * An agent that resolves conflict by finding new conflict-free plans.
 */
public abstract class PlanningAgent extends Agent {
	
	// Needed to overcome situation when the collision checker detected conflicts on the trajectory that was just returned by 
	// the best-response rutine.
	final int RADIUS_BUFFER_GRACE = 1;
	
	Logger LOGGER = Logger.getLogger(PlanningAgent.class);
	
	EvaluatedTrajectory trajectory;

	protected final int maxTime;
	protected final int waitMoveDuration;
	
	public int replanningCounter = 0;

	private HeuristicToGoal<tt.euclidtime3i.Point> heuristic;
	
	public PlanningAgent(String name, Point start, Point goal,
			Environment environment, int agentBodyRadius, int maxTime, int waitMoveDuration) {
		super(name, start, goal, environment, agentBodyRadius);
		this.maxTime = maxTime;
		this.waitMoveDuration = waitMoveDuration;
	}

	protected EvaluatedTrajectory getBestResponseTrajectory(
			Collection<tt.euclid2i.Region> staticObst,
			Collection<Region> dynamicObst, 
			tt.euclid2i.Point protectedPoint) {
		
		replanningCounter++;
		LOGGER.debug(getName() + " started planning ...");
		long startedAt = System.currentTimeMillis();

		EvaluatedTrajectory traj;
		LinkedList<tt.euclid2i.Region> sObstInflated = inflateStaticObstacles(staticObst, agentBodyRadius+RADIUS_BUFFER_GRACE);
		LinkedList<Region> dObstInflated = inflateDynamicObstacles(dynamicObst, agentBodyRadius+RADIUS_BUFFER_GRACE);
		dObstInflated = subtractProtectedPoint(dObstInflated, protectedPoint);
		

		if (getPlanningGraph() != null) {
			
			if (heuristic == null) {
				heuristic = new ShortestPathHeuristic(planningGraph, goal);
				//heuristic = new L2Heuristic(goal);
			}
			
			traj = BestResponse.computeBestResponse(start, goal, getPlanningGraph(), heuristic, sObstInflated, dObstInflated, maxTime, waitMoveDuration);
    	} else {
    		traj = BestResponse.computeBestResponse(start, goal, inflatedObstacles, environment.getBoundary().getBoundingBox(), sObstInflated, dObstInflated, maxTime, waitMoveDuration);
    	}

		LOGGER.debug(getName() + " finished planning in " + (System.currentTimeMillis() - startedAt) + "ms");
		return traj;
	}

	protected static LinkedList<tt.euclid2i.Region> inflateStaticObstacles(Collection<tt.euclid2i.Region> sObst, int radius) {
		// Inflate static obstacles
		LinkedList<tt.euclid2i.Region> sObstInflated = new LinkedList<tt.euclid2i.Region>();
		for (tt.euclid2i.Region region : sObst) {
			assert region instanceof Circle;
			Circle circle = (Circle) region;
			sObstInflated.add(new Circle(circle.getCenter(), circle.getRadius() + radius));
		}
		return sObstInflated;
	}

	protected static LinkedList<tt.euclidtime3i.Region> inflateDynamicObstacles(Collection<tt.euclidtime3i.Region> dObst, int radius) {
		// Inflate static obstacles
		LinkedList<tt.euclidtime3i.Region> dObstInflated = new LinkedList<tt.euclidtime3i.Region>();
		for (tt.euclidtime3i.Region region : dObst) {
			assert region instanceof MovingCircle;
			MovingCircle mc = (MovingCircle) region;
			dObstInflated.add(new MovingCircle(mc.getTrajectory(), mc.getRadius() + radius));
		}
		return dObstInflated;
	}
	
	protected static LinkedList<tt.euclidtime3i.Region> subtractProtectedPoint(Collection<tt.euclidtime3i.Region> dObst, tt.euclid2i.Point point) {
		// Inflate static obstacles
		LinkedList<tt.euclidtime3i.Region> dObstMinusPoint = new LinkedList<tt.euclidtime3i.Region>();
		for (tt.euclidtime3i.Region region : dObst) {
			assert region instanceof MovingCircle;
			MovingCircle mc = (MovingCircle) region;
			dObstMinusPoint.add(new MovingCircleMinusPoint(mc, point));
		}
		return dObstMinusPoint;
	}
}
