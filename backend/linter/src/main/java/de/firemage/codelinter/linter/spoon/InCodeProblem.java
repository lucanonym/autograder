package de.firemage.codelinter.linter.spoon;

import lombok.Getter;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;

public class InCodeProblem implements Problem {
    @Getter
    private final String qualifiedClassName;

    @Getter
    private final int line;

    @Getter
    private final int column;

    @Getter
    private final String description;

    @Getter
    private final ProblemCategory category;

    public InCodeProblem(CtClass<?> surroundingClass, SourcePosition position, String description, ProblemCategory category) {
        this.qualifiedClassName = surroundingClass.getQualifiedName();
        this.line = position.getLine();
        this.column = position.getColumn();
        this.description = description;
        this.category = category;
    }

    @Override
    public String toString() {
        return "InCodeProblem at " + qualifiedClassName + ":" + line + ". " + description;
    }
}