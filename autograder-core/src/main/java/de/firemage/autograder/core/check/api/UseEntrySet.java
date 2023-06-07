package de.firemage.autograder.core.check.api;

import de.firemage.autograder.core.LocalizedMessage;
import de.firemage.autograder.core.ProblemType;
import de.firemage.autograder.core.check.ExecutableCheck;
import de.firemage.autograder.core.dynamic.DynamicAnalysis;
import de.firemage.autograder.core.integrated.IntegratedCheck;
import de.firemage.autograder.core.integrated.SpoonUtil;
import de.firemage.autograder.core.integrated.StaticAnalysis;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.InvocationFilter;

import java.util.List;

@ExecutableCheck(reportedProblems = {ProblemType.STRING_IS_EMPTY_REIMPLEMENTED})
public class UseEntrySet extends IntegratedCheck {
    private static boolean hasInvokedKeySet(CtInvocation<?> ctInvocation) {
        return ctInvocation.getTarget() != null
            && ctInvocation.getExecutable() != null
            && SpoonUtil.isSubtypeOf(ctInvocation.getTarget().getType(), java.util.Map.class)
            && ctInvocation.getExecutable().getSimpleName().equals("keySet");
    }

    @Override
    protected void check(StaticAnalysis staticAnalysis, DynamicAnalysis dynamicAnalysis) {
        staticAnalysis.processWith(new AbstractProcessor<CtForEach>() {
            @Override
            public void process(CtForEach ctForEach) {
                if (ctForEach.isImplicit()
                    || !ctForEach.getPosition().isValidPosition()
                    || !(SpoonUtil.resolveCtExpression(ctForEach.getExpression()) instanceof CtInvocation<?> ctInvocation)
                    || !hasInvokedKeySet(ctInvocation)
                    || !ctForEach.getExpression().getPosition().isValidPosition()) {
                    return;
                }

                CtLocalVariable<?> loopVariable = ctForEach.getVariable();

                CtExecutableReference<?> ctExecutableReference = ctInvocation.getFactory()
                    .createCtTypeReference(java.util.Map.class)
                    .getTypeDeclaration()
                    .getMethod("get", ctInvocation.getFactory().createCtTypeReference(Object.class))
                    .getReference();

                List<CtInvocation<?>> invocations = ctForEach.getBody()
                    .getElements(new InvocationFilter(ctExecutableReference))
                    .stream()
                    .filter(invocation -> invocation.getTarget() != null
                        && invocation.getTarget().equals(ctInvocation.getTarget())
                        && invocation.getArguments().size() == 1
                        && invocation.getArguments().get(0) instanceof CtVariableAccess<?> ctVariableAccess
                        && ctVariableAccess.getVariable().equals(loopVariable.getReference()))
                    .toList();

                if (!invocations.isEmpty()) {
                    addLocalProblem(
                        ctForEach.getExpression(),
                        new LocalizedMessage("use-entry-set"),
                        ProblemType.STRING_IS_EMPTY_REIMPLEMENTED
                    );
                }
            }
        });
    }
}
