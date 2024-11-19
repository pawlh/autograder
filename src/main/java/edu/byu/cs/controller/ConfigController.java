package edu.byu.cs.controller;

import com.google.gson.JsonObject;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.service.ConfigService;
import edu.byu.cs.util.Serializer;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

import java.util.ArrayList;

public class ConfigController {

    public static final Handler getConfigAdmin = ctx -> {
        JsonObject response = ConfigService.getPrivateConfig();
        ctx.json(response);
    };

    public static final Handler getConfigStudent = ctx -> ctx.result(ConfigService.getPublicConfig().toString());

    public static final Handler updateLivePhases = ctx -> {
        JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
        ArrayList phasesArray = Serializer.deserialize(jsonObject.get("phases"), ArrayList.class);

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            throw new UnauthorizedResponse("No user credentials found");
        }

        ConfigService.updateLivePhases(phasesArray, user);
    };

    public static final Handler scheduleShutdown = ctx -> {
        User user = ctx.sessionAttribute("user");

        JsonObject jsonObject = ctx.bodyAsClass(JsonObject.class);
        String shutdownTimestampString = Serializer.deserialize(jsonObject.get("shutdownTimestamp"), String.class);
        Integer shutdownWarningMilliseconds = Serializer.deserialize(jsonObject.get("shutdownWarningMilliseconds"), Integer.class);

        try {
            ConfigService.scheduleShutdown(user, shutdownTimestampString);
            ConfigService.setShutdownWarningDuration(user, shutdownWarningMilliseconds);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
    };

    public static final Handler updateBannerMessage = ctx -> {
        User user = ctx.sessionAttribute("user");

        JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
        String expirationString = Serializer.deserialize(jsonObject.get("bannerExpiration"), String.class);

        String message = Serializer.deserialize(jsonObject.get("bannerMessage"), String.class);
        String link = Serializer.deserialize(jsonObject.get("bannerLink"), String.class);
        String color = Serializer.deserialize(jsonObject.get("bannerColor"), String.class);

        try {
            ConfigService.updateBannerMessage(user, expirationString, message, link, color);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
    };

    public static final Handler updateCourseIdsPost = ctx -> {
        SetCourseIdsRequest setCourseIdsRequest = ctx.bodyAsClass(SetCourseIdsRequest.class);

        User user = ctx.sessionAttribute("user");

        // Course Number
        try {
            ConfigService.updateCourseIds(user, setCourseIdsRequest);
        } catch (DataAccessException e) {
            ctx.status(400);
            ctx.result(e.getMessage());
        }
    };

    public static final Handler updateCourseIdsUsingCanvasGet = ctx -> {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            throw new UnauthorizedResponse("No user credentials found");
        }
        ConfigService.updateCourseIdsUsingCanvas(user);
    };
}
