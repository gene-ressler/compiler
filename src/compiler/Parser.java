package compiler;

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
