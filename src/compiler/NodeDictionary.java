package compiler;

import java.util.HashMap;

/**
 * A dictionary of DAG nodes already created.
 *
 * The DAG results because ancestors of already-created nodes are 
 * looked up in a dictionary, and the existing node used in place
 * of a newly created one. So each node may have any number of parents.
 */
class NodeDictionary {

    HashMap<Node, Node> map = new HashMap<>();

    /**
     * Look up the given new node in the dictionary using the common
     * subexpression definition of equality. Return the existing node
     * if it exists, otherwise the given one.
     * 
     * @param node a new node to look up in the dictionary
     * 
     * @return the existing equal node from the dictionary if it exists,
     * else the new one
     */
    Node lookup(Node node) {
        Node existing = map.get(node);
        if (existing == null) {
            map.put(node, node);
            return node;
        }
        return existing;
    }
}