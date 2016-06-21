package software.wings.resources;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.UserService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

// TODO: Auto-generated Javadoc

/**
 * Users Resource class.
 *
 * @author Rishi
 */
@Api("users")
@Path("/users")
@Timed
@ExceptionMetered
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
  @Inject private UserService userService;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<User>> list(@BeanParam PageRequest<User> pageRequest) {
    pageRequest.addFilter("appId", GLOBAL_APP_ID, EQ);
    return new RestResponse<>(userService.list(pageRequest));
  }

  /**
   * Register.
   *
   * @param user the user
   * @return the rest response
   */
  @POST
  public RestResponse<User> register(User user) {
    user.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(userService.register(user));
  }

  /**
   * Update.
   *
   * @param userId the user id
   * @param user   the user
   * @return the rest response
   */
  @PUT
  @Path("{userId}")
  public RestResponse<User> update(@QueryParam("appId") String appId, @PathParam("userId") String userId, User user) {
    user.setUuid(userId);
    user.setAppId(appId);
    return new RestResponse<>(userService.update(user));
  }

  /**
   * Delete.
   *
   * @param userId the user id
   * @return the rest response
   */
  @DELETE
  @Path("{userId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("userId") String userId) {
    userService.delete(userId);
    return new RestResponse();
  }

  /**
   * Gets the.
   *
   * @param userId the user id
   * @return the rest response
   */
  @GET
  @Path("{userId}")
  public RestResponse<User> get(@QueryParam("appId") String appId, @PathParam("userId") String userId) {
    return new RestResponse<>(userService.get(userId));
  }

  /**
   * Login.
   *
   * @param user the user
   * @return the rest response
   */
  @GET
  @Path("login")
  public RestResponse<User> login(@Auth User user) {
    return new RestResponse<>(user);
  }

  @GET
  @Path("verify/{token}")
  public RestResponse<User> verifyEmail(@PathParam("token") String token) {
    return new RestResponse<>(userService.verifyEmail(token));
  }

  /**
   * Assign role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the rest response
   */
  @PUT
  @Path("{userId}/role/{roleId}")
  public RestResponse<User> assignRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return new RestResponse<>(userService.addRole(userId, roleId));
  }

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the rest response
   */
  @DELETE
  @Path("{userId}/role/{roleId}")
  public RestResponse<User> revokeRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return new RestResponse<>(userService.revokeRole(userId, roleId));
  }
}
