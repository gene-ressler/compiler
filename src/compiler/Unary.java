package compiler;

import java.util.Set;

/**
 * Concrete unary operator nodes.
 * 
 * Equality for these nodes is defined consistent 
 * with common subexpression value.  Operators
 * of the same kind with exactly the same operand
 * are equal.
 */
class Unary extends Node {

    /**
     * The kind of operation represented by this node.
     */
    Kind kind;

    /**
     * Operand of this operator.
     */
    Node operand;

    /**
     * Kinds if unary nodes.
     */
    enum Kind {

        /**
         * Unary negation kind of unary node.
         */
        NEGATION("-");

        /**
         * Target code for causing this operation.
         */
        String code;

        /**
         * Construct a new unary node kind.
         *
         * @param code
         */
        Kind(String code) {
            this.code = code;
        }
    };

    /**
     * Construct a new unary operator with given kind and operand.
     *
     * @param kind kind of the operator
     * @param operand operand of the operator
     */
    public Unary(Kind kind, Node operand) {
        this.kind = kind;
        this.operand = operand;
    }

    @Override
    boolean canBeCommonSubexpression() {
        return true;
    }

    @Override
    Node countParents(Set<Node> visited) {
        ++parentCount;
        if (!visited.contains(this)) {
            visited.add(this);
            operand.countParents(visited);
        }
        return this;
    }

    @Override
    String emitCode(CodeGenerator ctx) {
        return ctx.addCodeFor(this, kind.code + "(" + operand.emitCode(ctx) + ")");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Unary) {
            Unary other = (Unary) obj;
            return kind == other.kind && operand == other.operand;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return kind.hashCode() ^ operand.hashCode();
    }

    @Override
    void print(int level, Set<Node> visited) {
        if (visited.contains(this)) {
            tabAndPrint(level, "ref to Unary " + kind + " (" + id + "," + parentCount + ")");
        } else {
            visited.add(this);
            tabAndPrint(level, "Unary " + kind + " (" + id + "," + parentCount + ")");
            tabAndPrint(level + 1, "operand:");
            operand.print(level + 2, visited);
        }
    }
}
