package compiler;

import java.io.IOException;
import java.io.Reader;

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
