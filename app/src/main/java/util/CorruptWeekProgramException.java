/**
 * @author HTI students, Spring 2013
 *
 */
package util;

public class CorruptWeekProgramException extends Exception {
    public CorruptWeekProgramException() {
        super();
    }

    public CorruptWeekProgramException(String msg) {
        super(msg);
    }

    public CorruptWeekProgramException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CorruptWeekProgramException(Throwable cause) {
        super(cause);
    }
}