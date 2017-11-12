package qirkat;

import org.junit.Test;
import ucb.junit.textui;

/** The suite of all JUnit tests for the qirkat package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    @Test
    public void testOne() {

    }
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(MoveTest.class, BoardTest.class,
                                      CommandTest.class));
    }

}


