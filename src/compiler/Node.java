package compiler;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstract node for a DAG. Other node types inherit.
 */
abstract class Node {

    /**
     * The last-used node id.
     */
    private static int lastId = 0;

    /**
     * Unique identifier for this node.
     */
    final int id;

    /**
     * Count of parents of this node.
     */
    int parentCount;

    /**
     * Construct a new node with unique ID number.
     */
    Node() {
        id = ++lastId;
        parentCount = 0;
    }

    /**
     * Return a temporary variable name for this node.
     */
    String getTmp() {
        return "t" + id;
    }

    /**
     * Tab out and print a string.
     *
     * @param level level of tabbing
     * @param s string to print
     */
    static void tabAndPrint(int level, String s) {
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        System.out.println(s);
    }

    /**
     * Print this node.
     *
     * @param level tabbing level to print at
     * @param visited set of nodes already visited
     */
    abstract void print(int level, Set<Node> visited);

    /**
     * Print the dag rooted at this node.
     */
    void print() {
        print(0, new HashSet<Node>());
    }

    /**
     * Return true iff this node should be subject to common subexpression
     * analysis.
     *
     * @return whether to consider this node as a potential common
     * subexpression.
     */
    abstract boolean canBeCommonSubexpression();

    /**
     * Set the parent count field.  Call this only once!
     * 
     * @param visited set of nodes already visited
     * @return this node
     */
    abstract Node countParents(Set<Node> visited);

    /**
     * Set the parent count field of the DAG rooted at this node.
     * Call only once!  (Currently in the parser.)
     */
    Node countParents() {
        return countParents(new HashSet<Node>());
    }

    /**
     * Append new statements to the context as needed and return a target
     * language expression for the value of this node.
     *
     * @param ctx code generation context
     * @return expression for the value of the node
     */
    abstract String emitCode(CodeGenerator ctx);
}
