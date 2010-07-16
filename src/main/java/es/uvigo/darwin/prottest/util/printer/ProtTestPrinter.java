package es.uvigo.darwin.prottest.util.printer;

import java.io.PrintWriter;
import java.util.Date;

import es.uvigo.darwin.prottest.ProtTest;
import es.uvigo.darwin.prottest.util.logging.ProtTestLogger;

/**
 * The Class ProtTestPrinter provides a common PrintWriter module to
 * the whole application. It encapsulates both output and error
 * PrintWriter.
 */
public class ProtTestPrinter {

    /** The output print writer. */
    private PrintWriter out;
    /** The error print writer. */
    private PrintWriter err;

    /**
     * Instantiates a new ProtTestPrinter.
     * 
     * @param out the output writer
     * @param err the error writer
     */
    public ProtTestPrinter(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Prints out the header section of the application.
     */
    public static void printHeader() {
        println("");
        println("");
        println("ProtTest-HPC - " + ProtTest.versionNumber + ProtTestFormattedOutput.space(10 - ProtTest.versionNumber.length(), ' ') +
                "                   Selection of models of protein evolution");
        println("(c) 2004-2010                     Diego Darriba, Guillermo Taboada, Ramón Doallo");
        println("                                  Federico Abascal, Rafael Zardoya, David Posada");
        println("(FA, DP)            Facultad de Biologia, Universidad de Vigo, 36200 Vigo, Spain");
        println("(FA, DP) Facultade de Informática, Universidade da Coruña, 15071 A Coruña, Spain");
        println("(RZ)                   Museo Nacional de Ciencias Naturales, 28006 Madrid, Spain");
        println("Contact:                 ddarriba@udc.es, fedeabascal@yahoo.es, dposada@uvigo.es");
        println("--------------------------------------------------------------------------------");
        println("");
        println((new Date()).toString());
        println("OS = " + System.getProperty("os.name") +
                " (" + System.getProperty("os.version") + ")");
        println("");
    }

    private static void println(String text) {
        ProtTestLogger.infoln(text, ProtTestPrinter.class);
    }

    /**
     * Prints out the footer section of the application.
     */
    public static void printFooter() {
        println("");
        println("");
        println("ProtTest-HPC - " + ProtTest.versionNumber);
    }

    /**
     * Gets the output writer.
     * 
     * @return the output writer
     */
    public PrintWriter getOutputWriter() {
        return out;
    }

    /**
     * Sets the output writer.
     * 
     * @param out the new output writer
     */
    public void setOutputWriter(PrintWriter out) {
        this.out = out;
    }

    /**
     * Gets the error writer.
     * 
     * @return the error writer
     */
    public PrintWriter getErrorWriter() {
        return err;
    }

    /**
     * Sets the error writer.
     * 
     * @param err the new error writer
     */
    public void setErrorWriter(PrintWriter err) {
        this.err = err;
    }

    /**
     * Flushes output writer.
     */
    public void flush() {
        out.flush();
    }

    /**
     * Flushes error writer.
     */
    public void flushError() {
        err.flush();
    }

    /**
     * Closes error writer.
     */
    public void closeError() {
        err.close();
    }
}
