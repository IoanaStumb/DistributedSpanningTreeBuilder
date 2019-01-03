# DistributedSpanningTreeBuilder
A Java implementation of a distributed spanning tree builder, using the flooding technique.

Usage (on Windows):
* start the cluster nodes using the file "src/startNodes.bat"
* start the client ("java client/Client")
* on the client, execute the commands "[external-port] request-tree" to request a tree from the [external-port] cluster node, or "[external-port] send-message [internal-port]" to ask the [external-port] cluster node to send a message to the [internal-port] cluster node