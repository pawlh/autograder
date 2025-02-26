package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.ClassCoverageAnalysis;
import edu.byu.cs.model.CoverageAnalysis;
import edu.byu.cs.util.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public class CoverageAnalyzer {
    private static final String COVERAGE_TESTED = "BRANCH";
    private static final String COVERAGE_MISSED_HEADER = COVERAGE_TESTED + "_MISSED";
    private static final String COVERAGE_COVERED_HEADER = COVERAGE_TESTED + "_COVERED";
    private static final String PACKAGE_HEADER = "PACKAGE";
    private static final String CLASS_HEADER = "CLASS";

    public CoverageAnalysis parse(File jacocoCsvOutput) throws GradingException {
        Collection<ClassCoverageAnalysis> classAnalyses = new HashSet<>();
        if(!jacocoCsvOutput.exists()) {
            return new CoverageAnalysis(classAnalyses);
        }

        String csv = FileUtils.readStringFromFile(jacocoCsvOutput);
        String[] lines = csv.split("\n");

        Integer classHeader = null;
        Integer packageHeader = null;
        Integer coverageMissedHeader = null;
        Integer coverageCoveredHeader = null;

        String[] headers = lines[0].split(",");
        for(int i = 0; i < headers.length; i++) {
            String s = headers[i].trim();
            switch (s) {
                case CLASS_HEADER -> classHeader = i;
                case PACKAGE_HEADER -> packageHeader = i;
                case COVERAGE_MISSED_HEADER -> coverageMissedHeader = i;
                case COVERAGE_COVERED_HEADER -> coverageCoveredHeader = i;
            }
        }

        if(classHeader == null || packageHeader == null || coverageMissedHeader == null || coverageCoveredHeader == null) {
            throw new GradingException("Error parsing coverage results");
        }

        for(int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");

            String className = values[classHeader].trim();
            String packageName = values[packageHeader].trim();
            int coverageMissed = Integer.parseInt(values[coverageMissedHeader].trim());
            int coverageCovered = Integer.parseInt(values[coverageCoveredHeader].trim());

            classAnalyses.add(new ClassCoverageAnalysis(className, packageName, coverageCovered, coverageMissed));
        }

        return new CoverageAnalysis(classAnalyses);
    }
}
