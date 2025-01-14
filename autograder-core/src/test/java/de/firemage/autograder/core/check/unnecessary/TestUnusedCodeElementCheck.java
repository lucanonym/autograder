package de.firemage.autograder.core.check.unnecessary;

import de.firemage.autograder.core.LinterException;
import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.Problem;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.AbstractCheckTest;
import de.firemage.autograder.core.compiler.JavaVersion;
import de.firemage.autograder.core.file.StringSourceInfo;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUnusedCodeElementCheck extends AbstractCheckTest {
    private static final String LOCALIZED_MESSAGE_KEY = "unused-element";
    private static final ProblemType PROBLEM_TYPE = ProblemType.UNUSED_CODE_ELEMENT;
    private static final List<ProblemType> PROBLEM_TYPES = List.of(PROBLEM_TYPE, ProblemType.UNUSED_CODE_ELEMENT_PRIVATE);

    private void assertEqualsUnused(String name, Problem problem) {
        assertEquals(
            this.linter.translateMessage(
                new LocalizedMessage(
                    LOCALIZED_MESSAGE_KEY,
                    Map.of("name", name)
                )),
            this.linter.translateMessage(problem.getExplanation())
        );
    }

    private static <T> void assertProblemSize(int expectedSize, Collection<T> problems) {
        assertEquals(
            expectedSize,
            problems.size(),
            "Expected %d problems, got %d: '%s'".formatted(expectedSize, problems.size(), problems)
        );
    }

    @Test
    void testUnusedField() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            Example example = new Example();
                            System.out.println(example);
                        }
                    }
                    """
                ),
                Map.entry(
                    "Example",
                    """
                        public class Example {
                            String exampleVariable;
                            String[] b;
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(2, problems);
        assertEqualsUnused("exampleVariable", problems.get(0));
        assertEqualsUnused("b", problems.get(1));
    }

    @Test
    void testUnusedFieldWithShadowing() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            int a; /*# not ok #*/
                            String[] b; /*# not ok #*/

                            void doSomething() { /*# not ok #*/
                                int a = 0; /*# not ok #*/
                                String[] b = new String[10]; /*# not ok #*/
                            }

                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(5, problems);
        assertEqualsUnused("a", problems.get(0));
        assertEqualsUnused("b", problems.get(1));
        assertEqualsUnused("doSomething", problems.get(2));
        assertEqualsUnused("a", problems.get(3));
        assertEqualsUnused("b", problems.get(4));
    }

    @Test
    void testUnusedRecursiveMethod() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                        public class Main {
                            void foo() { //# not ok
                                foo();
                            }

                            public static void main(String[] args) {}
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(1, problems);
        assertEqualsUnused("foo", problems.get(0));
    }

    @Test
    // See: https://github.com/Feuermagier/autograder/issues/228
    void testFieldUsedByInvocation() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            Graph graph = new Graph();
                            
                            System.out.println(graph.getRoot());
                        }
                    }
                    """
                ),
                Map.entry(
                    "Graph",
                    """
                        import java.util.ArrayList;

                        public class Graph {
                            private ArrayList<String> root;

                            public String getRoot() {
                                return this.root.get(0);
                            }
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertEquals(0, problems.size());
    }

    @Test
    void testUnusedTypeParameter() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            Graph<?, String> graph = new Graph<>();

                            graph.field = "Hello";

                            System.out.println(graph.field);
                        }
                    }
                    """
                ),
                Map.entry(
                    "Graph",
                    """
                        public class Graph<T, U> {
                            U field;
                        }
                        """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(1, problems);
        assertEqualsUnused("T", problems.get(0));
    }

    @Test
    void testOnlyWrittenVariable() throws LinterException, IOException {
        // For now, this is not detected as unused, because it might result in false positives
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            String arg1 = args[0];
                            String arg2 = args[1];
                            arg2 = "Hello";
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(1, problems);
        assertEqualsUnused("arg1", problems.get(0));
        // assertEqualsUnused("arg2", problems.get(1));
    }

    @Test
    void testUnusedMainMethod() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) { /*# ok; main method and args are always allowed #*/
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(0, problems);
    }

    @Test
    void testUnusedMainMethodDefaultPackage() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    // the main method can be called even if the class is only package visible
                    class Main {
                        public static void main(String[] args) {}
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(0, problems);
    }

    @Test
    void testUnusedExternalOverriddenMethod() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {}

                        @Override
                        public boolean equals(Object o) { /*# ok; one can not remove parameter of overridden method #*/
                            return true;
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(0, problems);
    }

    @Test
    void testIndirectlyUsedEnumVariant() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    import java.util.Arrays;

                    public class Main {
                        public static void main(String[] args) {
                            System.out.println(Arrays.asList(MyEnum.values()));
                        }
                    }
                    """
                ),
                Map.entry(
                    "MyEnum",
                    """
                    public enum MyEnum {
                        VARIANT;
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(0, problems);
    }

    @Test
    @Disabled("Unused types are not detected for now, because of potential false-positives")
    void testUnusedType() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {}
                        
                        private static class InnerClass {} // unused, not ok
                    }
                    """
                ),
                Map.entry(
                    "MyEnum",
                    """
                    public enum MyEnum { // not ok
                        VARIANT;
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(2, problems);
        assertEqualsUnused("InnerClass", problems.get(0));
        assertEqualsUnused("MyEnum", problems.get(1));
    }

    @Test
    void testUnusedConstructor() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public Main() {} //# not ok

                        public static void main(String[] args) {}
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(1, problems);
        assertEqualsUnused("Main()", problems.get(0));
    }

    @Test
    void testUnusedPrivateConstructor() throws LinterException, IOException {
        List<Problem> problems = this.check(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "StringUtils",
                    """
                    public class StringUtils {
                        private StringUtils() {}

                        public static void main(String[] args) {}
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertProblemSize(0, problems);
    }

    @Test
    void testUnusedPublicWithoutMain() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "StringUtils",
                    """
                    public class StringUtils {
                        String field;

                        public StringUtils() {} // ok

                        public static String repeat(String string, int n) { // ok
                            return string.repeat(n);
                        }
                        
                        void foo() {} // ok

                        public static String repeatable(String s, int n) { // ok (even though n is not used)
                            return s;
                        }
                        
                        private void helper() {} //# not ok
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        assertEqualsUnused("helper", problems.next());
        problems.assertExhausted();
    }

    @Test
    void testUsedPrivateStaticOverload() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "Tasks",
                    """
                    public class Tasks {
                        public static String invoke(String task) {
                            return invoke(task, task.length());
                        }
                        
                        private static String invoke(String task, int len) {
                            return task.repeat(len);
                        }
                    }
                    """
                ),
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            System.out.println(Tasks.invoke(args[0]));
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }

    @Test
    void testUsedGenericConstructor() throws LinterException, IOException {
        ProblemIterator problems = this.checkIterator(StringSourceInfo.fromSourceStrings(
            JavaVersion.JAVA_17,
            Map.ofEntries(
                Map.entry(
                    "And",
                    """
                    public class And<T, U> {
                        public And(T t, U u) {
                            System.out.println(t);
                            System.out.println(u);
                        }
                    }
                    """
                ),
                Map.entry(
                    "Main",
                    """
                    public class Main {
                        public static void main(String[] args) {
                            And<String, String> and = new And<>("Hello", "World");
                            System.out.println(and);
                        }
                    }
                    """
                )
            )
        ), PROBLEM_TYPES);

        problems.assertExhausted();
    }
}
