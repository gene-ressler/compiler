/**
 * An experiment in how DAGs work for common subexpression elimination.
 */
package compiler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

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
