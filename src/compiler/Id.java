package compiler;

import java.util.Set;

/**
 * Concrete identifier nodes.
 * 
 * Equality for these nodes is defined consistent 
 * with common subexpression value.  Identifiers
 * with the same string representation are equal.
 */
class Id extends Node {

    /**
     * The identifier's string representation.
     */
    String string;

    /**
     * Construct a new identifier.
     * 
     * @param string string representation 
     */
    Id(String string) {
        this.string = string;
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
        if (obj instanceof Id) {
            Id other = (Id) obj;
            return string.equals(other.string);
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
            tabAndPrint(level, "ref to Id '" + string + "' (" + id + "," + parentCount + ")");
        } else {
            visited.add(this);
            tabAndPrint(level, "Id '" + string + "' (" + id + "," + parentCount + ")");
        }
    }
}
