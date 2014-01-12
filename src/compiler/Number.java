package compiler;

import java.util.Set;

/**
 * Concrete numeric literal nodes.
 * 
 * Equality for these nodes is defined consistent 
 * with common subexpression value.  Numbers with
 * the same string representation or numeric value
 * are equal.
 */
class Number extends Node {

    /**
     * Token string from the program text.
     */
    String string;

    /**
     * Numeric value of the literal.
     */
    double val;

    /**
     * Construct a new numeric literal.
     * 
     * @param string numeric string from the program text 
     */
    Number(String string) {
        this.string = string;
        this.val = Double.valueOf(val);
    }

    @Override
    boolean canBeCommonSubexpression() {
        return false;
    }

    @Override
    Node countParents(Set<Node> visited) {
        ++parentCount;
        return this;
    }

    @Override
    String emitCode(CodeGenerator ctx) {
        return ctx.addCodeFor(this, string);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Number) {
            Number other = (Number) obj;
            return val == other.val;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    void print(int level, Set<Node> visited) {
        if (visited.contains(this)) {
            tabAndPrint(level, String.format("ref to Number %s (%d,%d)", string, id, parentCount));
        } else {
            visited.add(this);
            tabAndPrint(level, String.format("Number %s (%d,%d)", string, id, parentCount));
        }
    }
}
