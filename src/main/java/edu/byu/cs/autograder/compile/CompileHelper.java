package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.modifiers.PassoffJarModifier;
import edu.byu.cs.autograder.compile.modifiers.PomModifier;
import edu.byu.cs.autograder.compile.modifiers.TestFactoryModifier;
import edu.byu.cs.autograder.compile.verifers.*;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.util.ProcessUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CompileHelper {
    private final GradingContext gradingContext;

    public CompileHelper(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    private final Collection<StudentCodeVerifier> currentVerifiers =
            List.of(new ProjectStructureVerifier(), new ModuleIndependenceVerifier(), new ModifiedTestFilesVerifier(),
                    new TestLocationVerifier(), new ServerFacadeTestPortVerifier());


    private final Collection<StudentCodeModifier> currentModifiers =
            List.of(new PomModifier(), new PassoffJarModifier(), new TestFactoryModifier());

    public void compile() throws GradingException {
        verify();
        modify();
        packageRepo();
    }

    public void verify() throws GradingException {
        try {
            gradingContext.observer().update("Verifying code...");

            StudentCodeReader reader = StudentCodeReader.from(gradingContext);
            for(StudentCodeVerifier verifier : currentVerifiers) {
                verifier.verify(gradingContext, reader);
            }
        } catch (IOException e) {
            throw new GradingException("Failed to read project contents", e);
        }
    }

    public void modify() throws GradingException {
        for(StudentCodeModifier modifier : currentModifiers) {
            modifier.modify(gradingContext);
        }
    }


    /**
     * Packages the student repo into a jar
     */
    private void packageRepo() throws GradingException {
        gradingContext.observer().update("Compiling code...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(gradingContext.stageRepo());
        processBuilder.command("mvn", "package", "-DskipTests");
        try {
            ProcessUtils.ProcessOutput output = ProcessUtils.runProcess(processBuilder, 90000); //90 seconds
            if (output.statusCode() != 0) {
                String mavenError = getMavenError(output.stdOut());
                String notes = "Your Java source code could not be compiled. See Details.";

                Set<String> extraImports = NoExtraImportsVerifier.findExtraImports(mavenError);
                if (!extraImports.isEmpty()) {
                    notes += " It seems that you have imports from packages you cannot use. " +
                            "Double check the Phase specs for the proper packages to import. " +
                            "Please remove the following packages/imports from your code: " +
                            String.join(", ", extraImports) + "." ;
                }

                Rubric.Results results = Rubric.Results.textError(notes, mavenError);
                throw new GradingException("Failed to compile", results);
            }
        } catch (ProcessUtils.ProcessException ex) {
            throw new GradingException("Failed to compile: %s".formatted(ex.getMessage()), ex);
        }
    }

    /**
     * Retrieves maven error output from maven package stdout
     *
     * @param output A string containing maven standard output
     * @return A string containing maven package error lines
     */
    private String getMavenError(String output) {
        StringBuilder builder = new StringBuilder();
        for (String line : output.split("\n")) {
            if (line.contains("[ERROR] -> [Help 1]")) {
                break;
            }

            if(line.contains("[ERROR]")) {
                String trimLine = line.replace(gradingContext.stageRepo().getAbsolutePath(), "");
                builder.append(trimLine).append("\n");
            }
        }
        return builder.toString();
    }
}
