export enum Phase {
  Phase0,
  Phase1,
  Phase3,
  Phase4,
  Phase5,
  Phase6,
  Quality,
  GitHub,
}

export const listOfPhases = (): Array<Phase> => {
  let result = [];
  for (const phase in Phase) {
    const isValueProperty = Number(phase) >= 0;
    if (isValueProperty) {
      result.push(Phase[phase] as unknown as Phase);
    }
  }
  return result;
};

export type TestNode = {
  testName: string;
  passed: boolean;
  ecCategory: string;
  errorMessage: string;
  children: TestNode[];
  numTestsPassed: number;
  numTestsFailed: number;
  numExtraCreditPassed: number;
  numExtraCreditFailed: number;
};

export type ClassCoverageAnalysis = {
  className: string;
  packageName: string;
  covered: number;
  missed: number;
};

export type CoverageAnalysis = {
  classAnalyses: ClassCoverageAnalysis[];
};

export type TestResult = {
  root: TestNode;
  extraCredit: TestNode;
  coverage: CoverageAnalysis;
  error: string;
};

export type RubricItemResults = {
  notes: string;
  score: number;
  rawScore: number;
  possiblePoints: number;
  testResults: TestResult;
  textResults: string;
};

export type RubricItem = {
  category: string;
  criteria: string;
  results: RubricItemResults;
};

export type RubricType = "PASSOFF_TESTS" | "UNIT_TESTS" | "QUALITY" | "GIT_COMMITS" | "GITHUB_REPO";

export type RubricInfo = {
  id: string;
  points: number;
};

export type Rubric = {
  items: Record<RubricType, RubricItem>;
  passed: boolean;
  notes: string;
};

export type Submission = {
  netId: string;
  repoUrl: string;
  headHash: string;
  timestamp: string;
  phase: Phase;
  score: number;
  rawScore: number;
  notes: string;
  rubric: Rubric;
  passed: boolean;
  admin: boolean;
  verifiedStatus: VerifiedStatus;
};

export enum VerifiedStatus {
  Unapproved,
  ApprovedAutomatically,
  ApprovedManually,
  PreviouslyApproved,
}

export type User = {
  netId: string;
  firstName: string;
  lastName: string;
  repoUrl: string;
  role: "STUDENT" | "ADMIN";
};

export type CanvasSection = {
  id: number;
  name: string;
};

export type RepoUpdate = {
  timestamp: string;
  netId: string;
  repoUrl: string;
  adminUpdate: boolean;
  adminNetId: string | null;
};
