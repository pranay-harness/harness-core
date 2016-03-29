package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.service.intfc.UserService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 *  Users Resource class
 *
 *
 * @author Rishi
 *
 */

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
  public RestResponse<User> update(User user) {
    return new RestResponse<>(userService.update(user));
  }

  @DELETE
  @Path("{userID}")
  public void delete(@PathParam("userID") String userID) {
    userService.delete(userID);
  }

  @GET
  @Path("{userID}")
  public RestResponse<User> get(@PathParam("userID") String userID) {
    return new RestResponse<>(userService.get(userID));
  }

  @GET
  @Path("login")
  public RestResponse<User> login(@Auth User user) {
    return new RestResponse<>(user); // TODO: mask fields
  }

  @PUT
  @Path("{userID}/role/{roleID}")
  public RestResponse<User> assignRole(@PathParam("userID") String userID, @PathParam("roleID") String roleID) {
    return new RestResponse<>(userService.addRole(userID, roleID));
  }

  @DELETE
  @Path("{userID}/role/{roleID}")
  public RestResponse<User> revokeRole(@PathParam("userID") String userID, @PathParam("roleID") String roleID) {
    return new RestResponse<>(userService.revokeRole(userID, roleID));
  }
}
