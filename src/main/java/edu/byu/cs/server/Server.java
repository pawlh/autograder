package edu.byu.cs.server;

import edu.byu.cs.controller.WebSocketController;
import edu.byu.cs.controller.exception.*;
import edu.byu.cs.server.endpointprovider.EndpointProvider;
import io.javalin.Javalin;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.HandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Server {

    private Javalin app;

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private final EndpointProvider provider;

    public Server(EndpointProvider endpointProvider) {
        this.provider = endpointProvider;
    }

    public int start() {
        return start(8080);
    }

    public int start(int desiredPort) {
        int chosenPort = setupEndpoints(desiredPort);
        LOGGER.info("Server started on port {}", chosenPort);
        return chosenPort;
    }

    public void stop() {
        app.stop();
    }

    private int setupEndpoints(int port) {
        app = Javalin.create(config -> {
            if(getClass().getClassLoader().getResource("frontend/dist") != null) {
                config.staticFiles.add("/frontend/dist");
            } else {
                LOGGER.warn("Resource folder frontend/dist not found, not including static files");
            }

            config.jsonMapper(new SerializerAdapter());

            config.router.apiBuilder(() -> {
                before(provider.beforeAll());

                path("/auth", () -> {
                    get("/callback", provider.callbackGet());
                    get("/login", provider.loginGet());

                    // all routes after this point require authentication
                    post("/logout", provider.logoutPost());
                });

                path("/api", () -> {
                    before("/*", ctx -> {
                        if (ctx.method() != HandlerType.OPTIONS) provider.verifyAuthenticatedMiddleware().handle(ctx);
                    });

                    post("/repo", provider.setRepoUrl());

                    get("/submit", provider.submitGet());
                    post("/submit", provider.submitPost());

                    get("/latest", provider.latestSubmissionForMeGet());

                    get("/submission", provider.submissionXGet());
                    get("/submission/{phase}", provider.submissionXGet());

                    get("/me", provider.meGet());

                    get("/config", provider.getConfigStudent());

                    path("/admin", () -> {
                        before("/*", ctx -> {
                            if (ctx.method() != HandlerType.OPTIONS) provider.verifyAdminMiddleware().handle(ctx);
                        });

                        post("/repo/{netId}", provider.setRepoUrlAdmin());

                        get("/repo/history", provider.repoHistoryAdminGet());

                        get("/users", provider.usersGet());

                        post("/submit", provider.adminRepoSubmitPost());

                        path("/submissions", () -> {
                            post("/approve", provider.approveSubmissionPost());

                            get("/latest", provider.latestSubmissionsGet());

                            get("/latest/{count}", provider.latestSubmissionsGet());

                            get("/active", provider.submissionsActiveGet());

                            get("/student/{netId}", provider.studentSubmissionsGet());

                            post("/rerun", provider.submissionsReRunPost());
                        });

                        get("/test_mode", provider.testModeGet());

                        get("/analytics/commit", provider.commitAnalyticsGet());

                        get("/analytics/commit/{option}", provider.commitAnalyticsGet());

                        get("/honorChecker/zip/{section}", provider.honorCheckerZipGet());

                        get("/sections", provider.sectionsGet());

                        path("/config", () -> {
                            get("", provider.getConfigAdmin());

                            post("/phases", provider.updateLivePhases());
                            post("/phases/shutdown", provider.scheduleShutdown());

                            post("/banner", provider.updateBannerMessage());

                            post("/courseId", provider.updateCourseIdPost());
                            post("/reloadCourseIds", provider.reloadCourseAssignmentIds());

                            post("/penalties", provider.updatePenalties());
                        });
                    });
                });

                get("/*", provider.defaultGet());

                after(provider.afterAll());
            });
        })

        .options("/*", provider.defaultOptions())

        .ws("/ws", (wsConfig) -> {
            wsConfig.onError(WebSocketController::onError);
            wsConfig.onMessage(WebSocketController::onMessage);
        })

        .exception(BadRequestException.class, haltWithCode(400))
        .exception(UnauthorizedException.class, haltWithCode(401))
        .exception(ResourceForbiddenException.class, haltWithCode(403))
        .exception(ResourceNotFoundException.class, haltWithCode(404))
        .exception(WordOfWisdomViolationException.class, haltWithCode(418))
        .exception(UnprocessableEntityException.class, haltWithCode(422))
        .exception(Exception.class, haltWithCode(500))

        .start(port);

        return app.port();
    }

    private static <E extends Exception> ExceptionHandler<E> haltWithCode(int statusCode) {
        return (e, ctx) -> {
            ctx.status(statusCode);
            if (e.getMessage() != null) {
                ctx.result(e.getMessage());
            } else {
                ctx.result("An unknown %d error occurred.".formatted(statusCode));
            }
        };
    }
}
