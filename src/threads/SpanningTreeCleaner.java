package threads;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
		List<Integer> treesToRemove = new ArrayList<>();
		
		while (shouldRun) {
			try {
				Thread.sleep((long) checkTreesAfterMs);			
				Instant now = Instant.now();
				
				// check for trees to remove
				for (Integer key : spanningTrees.keySet()) {
					if (now.minusMillis(cleanTreeAfterMs).isAfter(spanningTrees.get(key).lastMessageSentAt)) {
						treesToRemove.add(key);
					}
				}
				
				if (!treesToRemove.isEmpty()) {
					System.out.println("[" + this.getName() + ":" + this.creatorNodePort + "]: I'm cleaning up old trees for nodes: ");
					treesToRemove.forEach(tree -> System.out.print(tree));
					System.out.println();
					
					for (Integer tree : treesToRemove) {
						spanningTrees.remove(tree);
						
						// if I am removing my own tree => reset my check values 
						if (tree == creatorNodePort) {
							creatorIsRoot.set(false);
							creatorIsBuildingTree.set(false);
						}
					}
					
					treesToRemove.clear();
				}
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
