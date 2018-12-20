package threads;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cluster.SpanningTree;

public class SpanningTreeCleaner extends Thread {
	
	private volatile boolean shouldRun = true;
	private int creatorNodePort;
	private AtomicBoolean creatorIsRoot;
	private AtomicBoolean creatorIsBuildingTree;
	private Map<Integer, SpanningTree> spanningTrees;
	private int checkTreesAfterMs;
	private int cleanTreeAfterMs;
	
	public SpanningTreeCleaner(String name, int creatorNodePort, AtomicBoolean creatorIsRoot, AtomicBoolean creatorIsBuildingTree, 
			Map<Integer, SpanningTree> spanningTrees, int checkTreesAfterMs, int cleanTreeAfterMs) {
		super(name);
		this.creatorNodePort = creatorNodePort;
		this.creatorIsRoot = creatorIsRoot;
		this.creatorIsBuildingTree = creatorIsBuildingTree;
		this.spanningTrees = spanningTrees;
		this.checkTreesAfterMs = checkTreesAfterMs;
		this.cleanTreeAfterMs = cleanTreeAfterMs;
	}
	
	@Override
	public void run() {
		while (shouldRun) {
			try {
				Thread.sleep((long) checkTreesAfterMs);
			} 
			catch (InterruptedException exception) {
				exception.printStackTrace();
				finish();
			}
		}
	}
	
	public void finish() {
		System.out.println("[" + this.getName() + "]: I am stopping.");		
		shouldRun = false;
	}
}
