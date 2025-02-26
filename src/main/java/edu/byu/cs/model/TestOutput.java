package edu.byu.cs.model;

public record TestOutput(TestNode root, TestNode extraCredit, CoverageAnalysis coverage, String error) {
}
