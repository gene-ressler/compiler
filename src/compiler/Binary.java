package compiler;

import java.util.Set;

/**
 * Concrete binary operator nodes.  
 * 
 * Equality for these nodes is defined consistent 
 * with common subexpression value.  Operators
 * of the same kind with exactly the same operands
 * are equal.
 */
class Binary extends Node {

    /**
     * The kind of operation represented by this node.
     */
    Kind kind;

    /**
     * The left-hand-side operand.
     */
    Node lhs;

    /**
     * The right-hand-side operand.
     */
    Node rhs;

    /**
     * Kinds of binary nodes.
     */
    enum Kind {

        /**
         * Logical or kind of binary node.  Commutative.
         */
        OR("||") {

            @Override
            Kind symmetricWith() {
                return OR;
            }    
        },
        /**
         * Logical and kind of binary node.  Commutative.
         */
        AND("&&") {

            @Override
            Kind symmetricWith() {
                return AND;
            }    
        },
        /**
         * Addition kind of binary node. Commutative.
         */
        ADD("+") {

            @Override
            Kind symmetricWith() {
                return ADD;
            }    
        },
        /**
         * Subtraction kind of binary node.
         */
        SUBTRACT("-") {

            @Override
            Kind symmetricWith() {
                return null;
            }    
        },
        /**
         * Multiplication kind of binary node. Commutative.
         */
        MULTIPLY("*") {

            @Override
            Kind symmetricWith() {
                return MULTIPLY;
            }    
        },
        /**
         * Division kind of binary node.
         */
        DIVIDE("/") {

            @Override
            Kind symmetricWith() {
                return null;
            }    
        },
        /**
         * Greater than comparison kind of binary node. Symmetric with LESS.
         */
        GREATER(">") {

            @Override
            Kind symmetricWith() {
                return LESS;
            }    
        },
        /**
         * Less than comparison kind of binary node. Symmetric with GREATER.
         */
        LESS("<") {

            @Override
            Kind symmetricWith() {
                return GREATER;
            }    
        },
        /**
         * Equality comparison kind of binary node. Commutative.
         */
        EQUALS("==") {

            @Override
            Kind symmetricWith() {
                return EQUALS;
            }    
        };

        /**
         * Target code to cause this operation.
         */
        String code;
        
        /**
         * Operation that the one represented by this kind is symmetric with.
         * 
         * If self, then the operator is commutative. If null then not 
         * symmetric with any other operator.
         * 
         * @return kind that this one is symmetric with
         */
        abstract Kind symmetricWith();

        /**
         * Construct a new binary node kind.
         *
         * @param code
         */
        Kind(String code) {
            this.code = code;
        }
    };

    /**
     * Construct a new binary node.
     *
     * @param kind kind of the node
     * @param lhs left-hand-side operand
     * @param rhs right-hand-side operand
     */
    public Binary(Kind kind, Node lhs, Node rhs) {
        this.kind = kind;
        this.lhs = lhs;
        this.rhs = rhs;
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
            lhs.countParents(visited);
            rhs.countParents(visited);
        }
        return this;
    }

    @Override
    String emitCode(CodeGenerator ctx) {
        return ctx.addCodeFor(this, "(" + lhs.emitCode(ctx) + ") " + kind.code + " (" + rhs.emitCode(ctx) + ")");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Binary) {
            Binary other = (Binary) obj;
            // Deal with both plain equality and symmetry/commutativity.
            return ((kind == other.kind && 
                        lhs == other.lhs && rhs == other.rhs) ||
                    (kind.symmetricWith() == other.kind &&
                        lhs == other.rhs && rhs == other.lhs));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return lhs.hashCode() ^ rhs.hashCode();
    }

    @Override
    void print(int level, Set<Node> visited) {
        if (visited.contains(this)) {
            tabAndPrint(level, "ref to Binary " + kind + " (" + id + "," + parentCount + ")");
        } else {
            visited.add(this);
            tabAndPrint(level, "Binary " + kind + " (" + id + "," + parentCount + ")");
            tabAndPrint(level + 1, "lhs:");
            lhs.print(level + 2, visited);
            tabAndPrint(level + 1, "rhs:");
            rhs.print(level + 2, visited);
        }
    }
}
