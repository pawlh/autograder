package edu.byu.cs.model;

import java.util.Collection;

public record CoverageAnalysis(Collection<ClassCoverageAnalysis> classAnalyses) {

}
