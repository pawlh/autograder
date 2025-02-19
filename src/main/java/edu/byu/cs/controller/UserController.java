package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.controller.exception.UnauthorizedException;
import edu.byu.cs.controller.exception.WordOfWisdomViolationException;
import edu.byu.cs.model.RepoUpdate;
import edu.byu.cs.model.User;
import edu.byu.cs.service.UserService;
import edu.byu.cs.util.Serializer;
import io.javalin.http.*;

import java.util.Collection;

public class UserController {

    public static final Handler setRepoUrl = ctx -> {
        User user = ctx.sessionAttribute("user");
        setRepoUrl(user.netId(), null, ctx);
        ctx.result( "Successfully updated repoUrl");
    };

    public static final Handler setRepoUrlAdmin = ctx -> {
        User admin = ctx.sessionAttribute("user");
        String studentNetId = ctx.pathParam(":netId");
        setRepoUrl(studentNetId, admin.netId(), ctx);
        ctx.result("Successfully updated repoUrl for user: " + studentNetId);
    };

    public static final Handler repoHistoryAdminGet = ctx -> {
        String repoUrl = ctx.queryParam("repoUrl");
        String netId = ctx.queryParam("netId");

        Collection<RepoUpdate> updates;
        try {
            updates = UserService.adminGetRepoHistory(repoUrl, netId);
        } catch (InternalServerException e) {
            throw new InternalServerErrorResponse("There was an internal server error getting repo updates");
        }
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result(Serializer.serialize(updates));
    };


    private static void setRepoUrl(String studentNetId, String adminNetId, Context ctx) {
        JsonObject jsonObject = new Gson().fromJson(ctx.body(), JsonObject.class);
        String repoUrl = new Gson().fromJson(jsonObject.get("repoUrl"), String.class);

        try {
            UserService.updateRepoUrl(studentNetId, repoUrl, adminNetId);
        } catch (BadRequestException e) {
            throw new BadRequestResponse(e.getMessage());
        }  catch (InternalServerException e) {
            throw new InternalServerErrorResponse(e.getMessage());
        } catch (WordOfWisdomViolationException e) {
            throw new ImATeapotResponse(e.getMessage());
        }

        ctx.status(200);
    }
}
