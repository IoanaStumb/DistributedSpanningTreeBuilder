# University homework

**This is a required homework for one of the [Faculty of Automatic Control and Computer Engineering Iasi](https://ac.tuiasi.ro/) master programmes.**

**Master programme:** [Distributed systems and Web technologies](https://ac.tuiasi.ro/studii/masterat/sisteme-distribuite-si-tehnologii-web/)

**Course:** High Performance Computing

**When:** Fall 2019

## DistributedSpanningTreeBuilder
A Java implementation of a distributed spanning tree builder, using the flooding technique.

Usage (on Windows):
* start the cluster nodes using the file "src/startNodes.bat"
* start the client ("java client/Client")
* on the client, execute the commands "[external-port] request-tree" to request a tree from the [external-port] cluster node, or "[external-port] send-message [internal-port]" to ask the [external-port] cluster node to send a message to the [internal-port] cluster node
* [external-port] and [internal-port] values must be defined in the "src/startNodes.bat" file
