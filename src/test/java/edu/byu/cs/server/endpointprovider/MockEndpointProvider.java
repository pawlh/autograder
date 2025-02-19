package edu.byu.cs.server.endpointprovider;

import io.javalin.http.Handler;
import io.javalin.http.Context;

import java.util.Map;

/**
 * A mock implementation of EndpointProvider designed for use with
 * Mockito.spy() (not mock()). Because individual endpoint methods only run once at
 * Server startup, they aren't useful for verifying endpoint calls.
 * MockEndpointProvider provides a runHandler() method that can be used for
 * that purpose.
 */
public class MockEndpointProvider implements EndpointProvider {

    /**
     * An empty function that is run by every endpoint, designed for use in
     * Mockito.verify() to verify endpoint calls. The individual endpoint
     * methods only run once on Server startup, so they can't be used
     * for that purpose.
     *
     * @param endpointName the name of the Handler that was called
     */
    public void runHandler(String endpointName) {}

    /**
     * An empty function that is run for each path parameter in each endpoint
     * that is called. It is designed for use with Mockito.verify() to verify
     * that endpoints are called with specific path parameters.
     *
     * @param endpointName the name of the Handler that was called
     * @param paramName    the name of the parameter
     * @param paramValue   the value of the parameter
     */
    public void hasPathParam(String endpointName, String paramName, String paramValue) {}

    private Object extractRequestInfo(String endpointName, Context ctx) {
        Map<String, String> params = ctx.pathParamMap();
        for (String paramName : params.keySet()) {
            String paramValue = params.get(paramName);
            this.hasPathParam(endpointName, paramName, paramValue);
        }

        this.runHandler(endpointName);

        return "{}";
    }

    @Override
    public Handler beforeAll() {
        return (ctx) -> extractRequestInfo("beforeAll", ctx);
    }

    @Override
    public Handler afterAll() {
        return (ctx) -> extractRequestInfo("afterAll", ctx);
    }

    @Override
    public Handler defaultGet() {
        return (ctx) -> extractRequestInfo("defaultGet", ctx);
    }

    @Override
    public Handler defaultOptions() {
        return (ctx) -> extractRequestInfo("defaultOptions", ctx);
    }

    @Override
    public Handler usersGet() {
        return (ctx) -> extractRequestInfo("usersGet", ctx);
    }

    @Override
    public Handler testModeGet() {
        return (ctx) -> extractRequestInfo("testModeGet", ctx);
    }

    @Override
    public Handler commitAnalyticsGet() {
        return (ctx) -> extractRequestInfo("commitAnalyticsGet", ctx);
    }

    @Override
    public Handler honorCheckerZipGet() {
        return (ctx) -> extractRequestInfo("honorCheckerZipGet", ctx);
    }

    @Override
    public Handler sectionsGet() {
        return (ctx) -> extractRequestInfo("sectionsGet", ctx);
    }

    @Override
    public Handler verifyAuthenticatedMiddleware() {
        return (ctx) -> extractRequestInfo("verifyAuthenticatedMiddleware", ctx);
    }

    @Override
    public Handler verifyAdminMiddleware() {
        return (ctx) -> extractRequestInfo("verifyAdminMiddleware", ctx);
    }

    @Override
    public Handler meGet() {
        return (ctx) -> extractRequestInfo("meGet", ctx);
    }

    @Override
    public Handler callbackGet() {
        return (ctx) -> extractRequestInfo("callbackGet", ctx);
    }

    @Override
    public Handler loginGet() {
        return (ctx) -> extractRequestInfo("loginGet", ctx);
    }

    @Override
    public Handler logoutPost() {
        return (ctx) -> extractRequestInfo("logoutPost", ctx);
    }

    @Override
    public Handler getConfigAdmin() {
        return (ctx) -> extractRequestInfo("getConfigAdmin", ctx);
    }

    @Override
    public Handler getConfigStudent() {
        return (ctx) -> extractRequestInfo("getConfigStudent", ctx);
    }

    @Override
    public Handler updateLivePhases() {
        return (ctx) -> extractRequestInfo("updateLivePhases", ctx);
    }

    @Override
    public Handler scheduleShutdown() {
        return (ctx) -> extractRequestInfo("scheduleShutdown", ctx);
    }

    @Override
    public Handler updateBannerMessage() {
        return (ctx) -> extractRequestInfo("updateBannerMessage", ctx);
    }

    @Override
    public Handler updateCourseIdPost() {
        return (ctx) -> extractRequestInfo("updateCourseIdPost", ctx);
    }

    @Override
    public Handler reloadCourseAssignmentIds() {
        return (ctx) -> extractRequestInfo("reloadCourseAssignmentIds", ctx);
    }

    @Override
    public Handler updatePenalties() {
        return (ctx) -> extractRequestInfo("updatePenalties", ctx);
    }

    @Override
    public Handler submitPost() {
        return (ctx) -> extractRequestInfo("submitPost", ctx);
    }

    @Override
    public Handler adminRepoSubmitPost() {
        return (ctx) -> extractRequestInfo("adminRepoSubmitPost", ctx);
    }

    @Override
    public Handler submitGet() {
        return (ctx) -> extractRequestInfo("submitGet", ctx);
    }

    @Override
    public Handler latestSubmissionForMeGet() {
        return (ctx) -> extractRequestInfo("latestSubmissionForMeGet", ctx);
    }

    @Override
    public Handler submissionXGet() {
        return (ctx) -> extractRequestInfo("submissionXGet", ctx);
    }

    @Override
    public Handler latestSubmissionsGet() {
        return (ctx) -> extractRequestInfo("latestSubmissionsGet", ctx);
    }

    @Override
    public Handler submissionsActiveGet() {
        return (ctx) -> extractRequestInfo("submissionsActiveGet", ctx);
    }

    @Override
    public Handler studentSubmissionsGet() {
        return (ctx) -> extractRequestInfo("studentSubmissionsGet", ctx);
    }

    @Override
    public Handler approveSubmissionPost() {
        return (ctx) -> extractRequestInfo("approveSubmissionPost", ctx);
    }

    @Override
    public Handler submissionsReRunPost() {
        return (ctx) -> extractRequestInfo("submissionsReRunPost", ctx);
    }

    @Override
    public Handler setRepoUrl() {
        return (ctx) -> extractRequestInfo("setRepoUrl", ctx);
    }

    @Override
    public Handler setRepoUrlAdmin() {
        return (ctx) -> extractRequestInfo("setRepoUrlAdmin", ctx);
    }

    @Override
    public Handler repoHistoryAdminGet() {
        return (ctx) -> extractRequestInfo("repoHistoryAdminGet", ctx);
    }
}
