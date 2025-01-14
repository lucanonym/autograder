package de.firemage.autograder.core.check.api;

import de.firemage.autograder.core.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.file.StringSourceInfo;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.compiler.JavaVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUseFormatString extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "use-format-string";
    private static final List<ProblemType> PROBLEM_TYPES = List.of(ProblemType.USE_FORMAT_STRING);

    void assertUseFormatString(String expected, Problem problem) {
        assertEquals(ProblemType.USE_FORMAT_STRING, problem.getProblemType());
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of(
                        "formatted", expected
                    )
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    @Test
    void testSimpleArrayCopy() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    private static final int MIN_NUMBER = 1;
                    private static final int MAX_NUMBER = 3;

                    public static void validateNumber(int number) {
                        if (number < MIN_NUMBER || number > MAX_NUMBER || number % 2 == 0) {
                            throw new IllegalArgumentException("Board must be an odd number between " + MIN_NUMBER + " and " + MAX_NUMBER);
                        }
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseFormatString(
            "\"Board must be an odd number between %d and %d\".formatted(MIN_NUMBER, MAX_NUMBER)",
            problems.next()
        );
        problems.assertExhausted();
    }

    @Test
    void testFormatStringBuilder() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceString(
            JavaVersion.JAVA_17,
            "Test",
            """
                public class Test {
                    public static void main(String[] args) {
                        StringBuilder stringBuilder = new StringBuilder();
                        
                        stringBuilder.append("[").append(args[0]).append("]");
                        
                        System.out.println(stringBuilder.toString());
                    }
                }
                """
        ), PROBLEM_TYPES);

        assertUseFormatString("stringBuilder.append(\"[%s]\".formatted(args[0]))", problems.next());
        problems.assertExhausted();
    }
}
