package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestOutput;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.*;

public class PassoffTestGrader extends TestGrader {

    public PassoffTestGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "passoff";
    }

    @Override
    protected Set<File> testsToCompile() {
        return Set.of(phaseTests);
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return PhaseUtils.passoffPackagesToTest(gradingContext.phase());
    }

    @Override
    protected Set<String> extraCreditTests() {
        return PhaseUtils.extraCreditTests(gradingContext.phase());
    }

    @Override
    protected String testName() {
        return "Passoff Tests";
    }

    @Override
    protected float getScore(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        float totalStandardTests = testResults.getNumTestsFailed() + testResults.getNumTestsPassed();
        TestNode extraCredit = testOutput.extraCredit();
        float totalECTests = extraCredit != null ? extraCredit.getNumTestsPassed() + extraCredit.getNumTestsFailed() : 0f;

        if (totalStandardTests == 0) return 0;

        float score = testResults.getNumTestsPassed() / totalStandardTests;
        if (totalECTests == 0) return score;

        // extra credit calculation
        if (score < 1f) return score;
        Map<String, Float> ecScores = getECScores(extraCredit);
        float extraCreditValue = PhaseUtils.extraCreditValue(gradingContext.phase());
        for (String category : extraCreditTests()) {
            if (ecScores.get(category) == 1f) {
                score += extraCreditValue;
            }
        }

        return score;
    }

    @Override
    protected String getNotes(TestOutput testOutput) {
        TestNode testResults = testOutput.root();
        StringBuilder notes = new StringBuilder();

        if (testResults == null) return "No tests were run";

        if (testResults.getNumTestsFailed() == 0) notes.append("All required tests passed");
        else notes.append("Some required tests failed");

        Map<String, Float> ecScores = getECScores(testOutput.extraCredit());
        float extraCreditValue = PhaseUtils.extraCreditValue(gradingContext.phase());
        float totalECPoints = ecScores.values().stream().reduce(0f, (f1, f2) -> (float) (f1 + Math.floor(f2))) * extraCreditValue;

        if (totalECPoints > 0f) notes.append("\nExtra credit tests: +").append(totalECPoints * 100).append("%");

        return notes.toString();
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.PASSOFF_TESTS;
    }


    private Map<String, Float> getECScores(TestNode results) {
        Map<String, Float> scores = new HashMap<>();
        if(results == null) return scores;

        Queue<TestNode> unchecked = new PriorityQueue<>();
        unchecked.add(results);

        while (!unchecked.isEmpty()) {
            TestNode node = unchecked.remove();
            for (TestNode child : node.getChildren().values()) {
                if (child.getEcCategory() != null) {
                    scores.put(child.getEcCategory(), (float) child.getNumTestsPassed() /
                            (child.getNumTestsPassed() + child.getNumTestsFailed()));
                    unchecked.remove(child);
                } else unchecked.add(child);
            }
        }

        return scores;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() {
        return Set.of();
    }
}
