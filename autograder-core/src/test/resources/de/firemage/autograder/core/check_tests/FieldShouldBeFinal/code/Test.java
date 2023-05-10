package de.firemage.autograder.core.check_tests.FieldShouldBeFinal.code;

public class Test {
    public static void main(String[] args) {}
}

class ShouldBeFinal {
    private int value = 0; // Not Ok
    private String value2; // Not Ok
    private String value3; // Ok

    public ShouldBeFinal() {
        this.value2 = "Hello World";
        this.value3 = "Hello World";
    }

    void foo() {
        this.value3 = "Value 3";
    }
}

abstract class A {
    private String value = null; // Ok

    protected A() {
    }

    protected A(String value) {
        this.value = value;
    }
}