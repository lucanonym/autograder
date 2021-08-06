package de.firemage.codelinter.linter.spotbugs;

import de.firemage.codelinter.linter.InCodeProblem;
import de.firemage.codelinter.linter.ProblemCategory;
import de.firemage.codelinter.linter.ProblemPriority;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.Confidence;
import java.io.File;

public class SpotbugsInCodeProblem extends InCodeProblem {
    private final BugInstance bug;

    public SpotbugsInCodeProblem(BugInstance bug, File root) {
        super(bug.getPrimaryClass().getSourceFileName(),
                bug.getPrimarySourceLineAnnotation().getStartLine(),
                -1,
                bug.getMessage(),
                ProblemCategory.OTHER,
                bug.getBugPattern().getShortDescription(),
                mapPriority(Confidence.getConfidence(bug.getPriority()))
        );
        this.bug = bug;
    }

    private static ProblemPriority mapPriority(Confidence confidence) {
        return switch (confidence) {
            case HIGH -> ProblemPriority.SEVERE;
            case MEDIUM -> ProblemPriority.FIX_RECOMMENDED;
            case LOW -> ProblemPriority.INFO;
            case IGNORE -> throw new IllegalArgumentException("confidence must not be 'ignore'");
        };
    }

    @Override
    public String getDisplayLocation() {
        return this.bug.getPrimaryClass().getSourceFileName();
    }
}
