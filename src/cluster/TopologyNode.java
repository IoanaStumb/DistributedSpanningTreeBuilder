package cluster;

public class TopologyNode {
	public int node;
	public int[] neighbors;
	
	public TopologyNode() {	
	}
	
	public TopologyNode(int node, int[] neighbors) {
		this.node = node;
		this.neighbors = neighbors;
	}
}
