package software.wings.resources;

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
import javax.ws.rs.core.MediaType;

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

  @GET
  public RestResponse<PageResponse<User>> list(@BeanParam PageRequest<User> pageRequest) {
    return new RestResponse<>(userService.list(pageRequest));
  }

  @POST
  public RestResponse<User> register(User user) {
    return new RestResponse<>(userService.register(user));
  }

  @PUT
  @Path("{userId}")
  public RestResponse<User> update(@PathParam("userId") String userId, User user) {
    user.setUuid(userId);
    return new RestResponse<>(userService.update(user));
  }

  @DELETE
  @Path("{userId}")
  public RestResponse delete(@PathParam("userId") String userId) {
    userService.delete(userId);
    return new RestResponse();
  }

  @GET
  @Path("{userId}")
  public RestResponse<User> get(@PathParam("userId") String userId) {
    return new RestResponse<>(userService.get(userId));
  }

  @GET
  @Path("login")
  public RestResponse<User> login(@Auth User user) {
    return new RestResponse<>(user); // TODO: mask fields
  }

  @PUT
  @Path("{userId}/role/{roleId}")
  public RestResponse<User> assignRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return new RestResponse<>(userService.addRole(userId, roleId));
  }

  @DELETE
  @Path("{userId}/role/{roleId}")
  public RestResponse<User> revokeRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return new RestResponse<>(userService.revokeRole(userId, roleId));
  }
}
