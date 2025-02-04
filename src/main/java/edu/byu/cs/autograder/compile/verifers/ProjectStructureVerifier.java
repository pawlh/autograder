package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that the project is structured correctly. The project should be at the top level of the git repository,
 * which is checked by looking for a pom.xml file
 */
public class ProjectStructureVerifier implements StudentCodeVerifier {
    private static final HashMap<String, String> customMessages = new HashMap<>();
    private static final Set<String> filePaths = new HashSet<>();
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        verifyRootPom(context);
        verifyDirectoryStructure(context);
    }

    private void verifyRootPom(GradingContext context) throws GradingException {
        File pomFile = new File(context.stageRepo(), "pom.xml");
        if (!pomFile.exists()) {
            throw new GradingException("Project is not structured correctly. Your project should be at the top level of your git repository.");
        }
    }

    private void addFilePaths(String filePath, String customMessage){
        filePaths.add(filePath);
        customMessages.put(filePath, customMessage);
    }

    private void addFilePaths(String filePath){
        addFilePaths(filePath, "");
    }

    private void verifyDirectoryStructure(GradingContext context) {

        switch (context.phase()) {
            case Phase5, Phase6:
                addFilePaths("client/src/test/java");
            case Phase3, Phase4:
                addFilePaths("server/src/test/java");
                addFilePaths("server/src/main/resources");
                addFilePaths("server/src/main/resources/web",
                        " This may lead to the autograder giving different results for the \"static files\" test." +
                                " Ensure the files are in the specified location and committed and pushed to your repository.");
            default:
                addFilePaths("shared/src/main/java");
                addFilePaths("shared/src/test/java");
                addFilePaths("server/src/main/java");
                addFilePaths("client/src/main/java");
        }

        for(String filePath : filePaths) {
            File file = new File(context.stageRepo(), filePath);
            if(!file.exists() || !file.isDirectory()) {
                context.observer().notifyWarning("Directory %s could not be found.".formatted(filePath)
                        + customMessages.get(filePath));
            }
        }
    }
}
