package edu.byu.cs.model;

public record ClassCoverageAnalysis(String className, String packageName, int covered, int missed) {
}
