/**
 * An experiment in how DAGs work for common subexpression elimination.
 */
package compiler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Expression compilers with common subexpression elimination (CSE).
 */
public class Compiler {

    /**
     * Compile a simple expression in the input file into code with common
     * subexpressions computed only once.
     *
     * @param args program argument: a file name
     */
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new BufferedReader(new FileReader(args[0])));
            try {
                Parser parser = new Parser(scanner);
                Node dag = parser.parseToDag();
                dag.print();
                new CodeGenerator(dag).emitCode();
            } catch (Parser.ParseError ex) {
                System.err.println("Parse error: " + ex.getMessage());
            }
        } catch (FileNotFoundException | ArrayIndexOutOfBoundsException ex) {
            System.err.println("Bad or missing input file name.");
        }
    }
}

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
         * Logical or kind of binary node.
         */
        OR("||"),
        /**
         * Logical and kind of binary node.
         */
        AND("&&"),
        /**
         * Addition kind of binary node.
         */
        ADD("+"),
        /**
         * Subtraction kind of binary node.
         */
        SUBTRACT("-"),
        /**
         * Multiplication kind of binary node.
         */
        MULTIPLY("*"),
        /**
         * Division kind of binary node.
         */
        DIVIDE("/"),
        /**
         * Greater than comparison kind of binary node.
         */
        GREATER(">"),
        /**
         * Less than comparison kind of binary node.
         */
        LESS("<"),
        /**
         * Equality comparison kind of binary node.
         */
        EQUALS("==");

        /**
         * Target code to cause this operation.
         */
        String code;

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
            if (kind != other.kind) {
                return false;
            }
            if (lhs == other.lhs && rhs == other.rhs) {
                return true;
            }
            // Now allow for commutativity.
            if (kind != Kind.ADD && kind != Kind.MULTIPLY) {
                return false;
            }
            return lhs == other.rhs && rhs == other.lhs;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return kind.hashCode() ^ lhs.hashCode() ^ rhs.hashCode();
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

/**
 * Simple recursive descent expression parser that produces DAGs as output.
 */
class Parser {

    /**
     * Scanner for obtaining tokens from the input.
     */
    private final Scanner scanner;

    /**
     * THe lookahead token.
     */
    private Scanner.Token lookAhead;

    /**
     * The node dictionary for constructing a DAG of the input.
     */
    private final NodeDictionary dict;

    /**
     * Exception raised when the parser sees a syntax error.
     */
    class ParseError extends Exception {

        ParseError(String msg) {
            super(msg);
        }
    }

    /**
     * Construct a new parser with given scanner for input. It will produce a
     * DAG as output.
     */
    Parser(Scanner scanner) {
        this.scanner = scanner;
        this.dict = new NodeDictionary();
        advance();
    }

    /**
     * Run the parser on the scanner's input and return the resulting DAG or
     * throw a ParseError exception.
     *
     * @return root node of DAG
     * @throws compiler.Parser.ParseError
     */
    Node parseToDag() throws ParseError {
        return disjunction().countParents();
    }

    /**
     * Any number of perands connected by "or" operations.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node disjunction() throws ParseError {
        Node lhs = conjunction();
        while (lookAhead == Scanner.Token.OR) {
            advance();
            Node rhs = conjunction();
            lhs = dict.lookup(new Binary(Binary.Kind.OR, lhs, rhs));
        }
        return lhs;
    }

    /**
     * Any number of operands connected by "and" operations.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node conjunction() throws ParseError {
        Node lhs = comparison();
        while (lookAhead == Scanner.Token.AND) {
            advance();
            Node rhs = comparison();
            lhs = dict.lookup(new Binary(Binary.Kind.AND, lhs, rhs));
        }
        return lhs;
    }

    /**
     * Two operands connected by a comparison.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node comparison() throws ParseError {
        Node lhs = numeric();
        Binary.Kind kind;
        switch (lookAhead) {
            case LESS:
                kind = Binary.Kind.LESS;
                break;
            case GREATER:
                kind = Binary.Kind.GREATER;
                break;
            case EQUALS:
                kind = Binary.Kind.EQUALS;
                break;
            default:
                return lhs;
        }
        advance();
        Node rhs = numeric();
        return dict.lookup(new Binary(kind, lhs, rhs));
    }

    /**
     * Any number of operands connected by additive operators.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node numeric() throws ParseError {
        Node lhs = term();
        while (lookAhead == Scanner.Token.PLUS || lookAhead == Scanner.Token.MINUS) {
            Binary.Kind kind = lookAhead == Scanner.Token.PLUS ? Binary.Kind.ADD : Binary.Kind.SUBTRACT;
            advance();
            Node rhs = term();
            lhs = dict.lookup(new Binary(kind, lhs, rhs));
        }
        return lhs;
    }

    /**
     * Any number of operands connected by multiplicative operators.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node term() throws ParseError {
        Node lhs = signedFactor();
        while (lookAhead == Scanner.Token.STAR || lookAhead == Scanner.Token.SLASH) {
            Binary.Kind kind = lookAhead == Scanner.Token.STAR ? Binary.Kind.MULTIPLY : Binary.Kind.DIVIDE;
            advance();
            Node rhs = term();
            lhs = dict.lookup(new Binary(kind, lhs, rhs));
        }
        return lhs;

    }

    /**
     * A single operand with an optional leading minus sign.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node signedFactor() throws ParseError {
        boolean minus = false;
        if (lookAhead == Scanner.Token.MINUS) {
            advance();
            minus = true;
        }
        Node operand = factor();
        return minus ? dict.lookup(new Unary(Unary.Kind.NEGATION, operand)) : operand;
    }

    /**
     * An atomic operand or any expression in parentheses.
     * 
     * @return expression DAG root
     * @throws compiler.Parser.ParseError 
     */
    private Node factor() throws ParseError {
        switch (lookAhead) {
            case LEFT_PAREN:
                advance();
                Node disjunction = disjunction();
                match(Scanner.Token.RIGHT_PAREN);
                return disjunction;
            case ID:
                Node id = dict.lookup(new Id(scanner.getTokenString()));
                advance();
                return id;
            case NUMBER:
                Node number = dict.lookup(new Number(scanner.getTokenString()));
                advance();
                return number;
            default:
                throw new ParseError("Expected a factor.");
        }
    }

    /**
     * Advance the parser by reading the next token into the lookahead.
     */
    private void advance() {
        lookAhead = scanner.next();
    }

    /**
     * Check the lookahead for equality with the given token.
     * 
     * Advance if the lookahead matches, otherwise raise a syntax error.
     * 
     * @param token token to meach
     * @throws compiler.Parser.ParseError 
     */
    private void match(Scanner.Token token) throws ParseError {
        if (lookAhead != token) {
            throw new ParseError("Expected a " + token + ". Found a " + lookAhead + ".");
        }
        advance();
    }
}

/**
 * Expression token scanners.
 */
class Scanner {

    /**
     * The scanner text input stream.
     */
    private final Reader input;
    
    /**
     * The current character from the input.
     */
    private final char[] buf = new char[1];
    
    /**
     * A character that represents end of input.
     */
    private static final char EOF_MARK = Character.MIN_VALUE;
    
    /**
     * The current discrete finite automaton (DFA) state.
     */
    private State state;
    
    /**
     * A place to store the text of tokens with distinct text representations.
     */
    private final StringBuilder tokenString;

    /**
     * Tokens returned by the scanner.
     */
    enum Token {

        /**
         * Any identifier.  Text is stored.
         */
        ID,
        
        /**
         * Any numeric literal.  Text is stored.
         */
        NUMBER,
        
        /**
         * A plus sign.
         */
        PLUS,
        
        /**
         * A minus sign.
         */
        MINUS,
        
        /**
         * An asterisk.
         */
        STAR,
        
        /**
         * A slash.
         */
        SLASH,
        
        /**
         * A less than sign.
         */
        LESS,
        
        /**
         * A greater than sign.
         */
        GREATER,
        
        /**
         * An equality comparison sign.
         */
        EQUALS,
        
        /**
         * An "and" logical operator sign.
         */
        AND,
        
        /**
         * An "or" logical operator sign.
         */
        OR,
        
        /**
         * A left parenthesis.
         */
        LEFT_PAREN,
        
        /**
         * A right parenthesis.
         */
        RIGHT_PAREN,
        
        /**
         * A token representing end of input.
         * 
         * After this is returned, the scanner should not be called again.
         */
        END_OF_INPUT,
        
        /**
         * A token representing erroneous input.
         * 
         * This includes illegal characters and 
         * partial tokens followed by end-of-input.
         */
        ERROR,
    }

    /**
     * DFA state.
     */
    private enum State {

        /**
         * Initial state.
         */
        START,
        
        /**
         * Some non-empty prefix of an identifier has been seen.
         */
        IN_ID,
        
        /**
         * Some non-empty prefix of a numeric literal has been seen.
         */
        IN_NUMBER,
        
        /**
         * Some non-empty prefix of a decimal fraction has been seen.
         */
        IN_FRACTION,
        
        /**
         * The first of two equal signs representing an equals
         * comparison sign has been seen.
         */
        IN_EQUALS,
        
        /**
         * The first of two ampersands representing a logical "and"
         * operator has been seen.
         */
        IN_AND,
        
        /**
         * The first of two pipes representing a logical "or"
         * operator has been seen.
         */
        IN_OR,
        
        /**
         * An error has been seen in the input.
         */
        ERROR,
    }

    /**
     * Construct a new scanner.
     * 
     * @param input text input stream
     */
    Scanner(Reader input) {
        this.tokenString = new StringBuilder();
        this.input = input;
        advance();
    }

    /**
     * Return the next token from the input stream.
     * 
     * This implements a little DFA to identify tokens.
     * 
     * @return the next token from the input
     */
    Token next() {
        clear();
        state = State.START;
        while (true) {
            switch (state) {
                case START:
                    if (Character.isWhitespace(peek())) {
                        advance();
                    } else if (Character.isDigit(peek())) {
                        append();
                        state = State.IN_NUMBER;
                    } else if (Character.isJavaIdentifierStart(peek())) {
                        append();
                        state = State.IN_ID;
                    } else {
                        switch (peek()) {
                            case EOF_MARK:
                                return Token.END_OF_INPUT;
                            case '&':
                                advance();
                                state = State.IN_AND;
                                break;
                            case '|':
                                advance();
                                state = State.IN_OR;
                                break;
                            case '=':
                                advance();
                                state = State.IN_EQUALS;
                                break;
                            case '+':
                                advance();
                                return Token.PLUS;
                            case '-':
                                advance();
                                return Token.MINUS;
                            case '*':
                                advance();
                                return Token.STAR;
                            case '/':
                                advance();
                                return Token.SLASH;
                            case '>':
                                advance();
                                return Token.GREATER;
                            case '<':
                                advance();
                                return Token.LESS;
                            case '(':
                                advance();
                                return Token.LEFT_PAREN;
                            case ')':
                                advance();
                                return Token.RIGHT_PAREN;
                            default:
                                state = State.ERROR;
                                break;
                        }
                    }
                    break;
                case IN_ID:
                    if (Character.isJavaIdentifierPart(peek())) {
                        append();
                    } else if (Character.isIdentifierIgnorable(peek())) {
                        advance();
                    } else {
                        return Token.ID;
                    }
                    break;
                case IN_NUMBER:
                    if (Character.isDigit(peek())) {
                        append();
                    } else if (peek() == '.') {
                        append();
                        state = State.IN_FRACTION;
                    } else {
                        return Token.NUMBER;
                    }
                    break;
                case IN_FRACTION:
                    if (Character.isDigit(peek())) {
                        append();
                    } else {
                        return Token.NUMBER;
                    }
                    break;
                case IN_EQUALS:
                    if (peek() == '=') {
                        advance();
                        return Token.EQUALS;
                    } else {
                        state = State.ERROR;
                    }
                    break;
                case IN_AND:
                    if (peek() == '&') {
                        advance();
                        return Token.AND;
                    } else {
                        state = State.ERROR;
                    }
                    break;
                case IN_OR:
                    if (peek() == '|') {
                        advance();
                        return Token.OR;
                    } else {
                        state = State.ERROR;
                    }
                    break;
                case ERROR:
                    return Token.ERROR;
            }
        }
    }

    /**
     * Get the string representation of the token, if it has one.
     * 
     * See Token for those that do.
     * 
     * @return string representation of the most recently scanned token
     */
    String getTokenString() {
        return tokenString.toString();
    }

    /**
     * Peek at the current character on the input stream without changing it.
     * 
     * @return current character on the input stream
     */
    private char peek() {
        return buf[0];
    }

    /**
     * Clear the token string buffer.
     */
    private void clear() {
        tokenString.setLength(0);
    }

    /**
     * Append the current character and advance the input stream.
     */
    private void append() {
        tokenString.append(buf[0]);
        advance();
    }

    /**
     * Advance the input stream, replacing the current character.
     */
    private void advance() {
        try {
            if (input.read(buf) < 1) {
                buf[0] = EOF_MARK;
            }
        } catch (IOException ex) {
            System.err.println("IO error");
            buf[0] = EOF_MARK;
        }
    }
}
