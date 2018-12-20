package cluster;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SpanningTree {
	public int id; // root
	public int parentPort; // the specific node's parent in this tree
	public Set<Integer> childrenPorts;
	public Set<Integer> otherPorts;
	public String treeExpression;
	
	// meta information
	public boolean receivedResponsesFromAllNeighbors;
	public Map<Integer, String> finishedChildrenResponses;
	public Instant lastMessageSentAt;
	
	public SpanningTree(int rootPort) {
		this.id = rootPort;
		this.parentPort = -1;
		this.treeExpression = "";
		
		this.childrenPorts = new HashSet<>();
		this.otherPorts = new HashSet<>();
		
		this.receivedResponsesFromAllNeighbors = false;
		this.finishedChildrenResponses = new HashMap<>();
		this.lastMessageSentAt = Instant.now();
	}
	
	public String buildAndSetTreeExpression(int creatorNodePort) {
		String expression;
			
		if (childrenPorts.size() == 0) {
			// if no children, return empty tree expression
			// {"1206": []}
			expression = String.format("{\"%d\": []}", creatorNodePort);
		}
		else {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(String.format("{\"%d\": [", creatorNodePort));
			
			List<String> childrenExpressionTrees = 
					this.finishedChildrenResponses.entrySet().stream()
					.map(entry -> entry.getValue())
					.collect(Collectors.toList());
		    String joinResult = String.join(", ", childrenExpressionTrees);

		    stringBuilder.append(joinResult);
		    stringBuilder.append("]}");
		    
			expression = stringBuilder.toString();
			
			// System.out.println(stringBuilder.toString());
		}
		
		this.treeExpression = expression;
		return expression;
	}
}
