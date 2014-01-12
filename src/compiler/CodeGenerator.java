package compiler;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A code generator context.
 * 
 * Includes the dag to convert, storage of emitted code, and helper functions.
 */
class CodeGenerator {

    /**
     * DAG to generate code for.
     */
    final Node dag;

    /**
     * Buffer for lines of code.
     */
    final ArrayList<String> lines;

    /**
     * Nodes that have already been visited during code generation.
     */
    final HashSet<Node> visited;

    /**
     * Construct a new code generation context.
     *
     * @param dag DAG to generate code for.
     */
    public CodeGenerator(Node dag) {
        this.dag = dag;
        this.lines = new ArrayList<>();
        this.visited = new HashSet<>();
    }

    /**
     * Helper to add code for one node.
     * 
     * The value of the node is computed by the expression in the given string. 
     * The emitted code either places the expression's value in a temporary 
     * variable and returns that variable for future reference as a common 
     * subexpression or it returns the expression outright.
     *
     * @param node node to add code to the context for
     * @param expr expression that will compute the node's value
     * @return expression representing this node (either expr or a temp var)
     */
    String addCodeFor(Node node, String expr) {
        if (node.canBeCommonSubexpression()) {
            if (visited.contains(node)) {
                return node.getTmp();
            }
            visited.add(node);
            if (node.parentCount > 1) {
                lines.add(node.getTmp() + " = " + expr + ";");
                return node.getTmp();
            }
        }
        return expr;
    }

    /**
     * Emit code for the DAG in this context.
     */
    void emitCode() {
        String expr = dag.emitCode(this);
        for (String line : lines) {
            System.out.println(line);
        }
        System.out.println("return " + expr + ";");
    }
}
