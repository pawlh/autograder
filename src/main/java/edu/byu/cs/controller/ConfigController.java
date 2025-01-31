package edu.byu.cs.controller;

import com.google.gson.JsonObject;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.model.request.ConfigHolidayUpdateRequest;
import edu.byu.cs.model.request.ConfigPenaltyUpdateRequest;
import edu.byu.cs.service.ConfigService;
import edu.byu.cs.util.Serializer;
import spark.Route;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.halt;

public class ConfigController {

    public static final Route getConfigAdmin = (req, res) -> {
        try {
            PrivateConfig config = ConfigService.getPrivateConfig();
            res.status(200);
            return Serializer.serialize(config);
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
            return res;
        }
    };

    public static final Route getConfigStudent = (req, res) -> {
        PublicConfig config = ConfigService.getPublicConfig();

        res.status(200);
        return Serializer.serialize(config);
    };

    public static final Route updateLivePhases = (req, res) -> {
        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        ArrayList phasesArray = Serializer.deserialize(jsonObject.get("phases"), ArrayList.class);
        User user = req.session().attribute("user");

        ConfigService.updateLivePhases(phasesArray, user);

        res.status(200);
        return "";
    };

    public static final Route scheduleShutdown = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        String shutdownTimestampString = Serializer.deserialize(jsonObject.get("shutdownTimestamp"), String.class);
        Integer shutdownWarningMilliseconds = Serializer.deserialize(jsonObject.get("shutdownWarningMilliseconds"), Integer.class);

        try {
            ConfigService.scheduleShutdown(user, shutdownTimestampString);
            ConfigService.setShutdownWarningDuration(user, shutdownWarningMilliseconds);
        } catch (DataAccessException e) {
            halt(500, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            halt(400, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

    public static final Route updateBannerMessage = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        String expirationString = Serializer.deserialize(jsonObject.get("bannerExpiration"), String.class);

        String message = Serializer.deserialize(jsonObject.get("bannerMessage"), String.class);
        String link = Serializer.deserialize(jsonObject.get("bannerLink"), String.class);
        String color = Serializer.deserialize(jsonObject.get("bannerColor"), String.class);

        try {
            ConfigService.updateBannerMessage(user, expirationString, message, link, color);
        } catch (IllegalArgumentException e) {
            halt(400, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

    public static final Route updateCourseIdPost = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        Integer courseId = Serializer.deserialize(jsonObject.get("courseId"), Integer.class);

        try {
            ConfigService.setCourseId(user, courseId);
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
        } catch (CanvasException e) {
            res.status(400);
            res.body("Canvas Error:\nEither the Canvas Course #%d doesn't exist, the Autograder doesn't have access to the course, or Canvas was unable to be reached.".formatted(courseId));
        }
        return "";
    };

    public static final Route updateCourseIdsUsingCanvasGet = (req, res) -> {
        User user = req.session().attribute("user");
        ConfigService.updateCourseIdsUsingCanvas(user);
        res.status(200);
        return "";
    };

    public static final Route updatePenalties = (req, res) -> {
        User user = req.session().attribute("user");

        ConfigPenaltyUpdateRequest request = Serializer.deserialize(req.body(), ConfigPenaltyUpdateRequest.class);

        try {
            ConfigService.processPenaltyUpdates(user, request);
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
        }

        return "";
    };

    public static final Route updateHolidays = (req, res) -> {
        User user = req.session().attribute("user");

        ConfigHolidayUpdateRequest request = Serializer.deserialize(req.body(), ConfigHolidayUpdateRequest.class);

        List<LocalDate> holidays = new ArrayList<>();
        try {
            for (String date : request.holidays()) {
                holidays.add(LocalDate.parse(date));
            }

            ConfigService.updateHolidays(user, holidays);
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
        } catch (DateTimeParseException e) {
            res.status(400);
            res.body("Invalid date format provided.");
        }

        return "";
    };
}
