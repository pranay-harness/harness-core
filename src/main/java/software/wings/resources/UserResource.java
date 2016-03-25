package software.wings.resources;

import static software.wings.security.PermissionAttr.USER;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import io.dropwizard.auth.Auth;
import software.wings.beans.Base;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.annotations.AuthRule;
import software.wings.service.UserService;

/**
 *  Users Resource class
 *
 *
 * @author Rishi
 *
 */
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource extends Base {
  private UserService userService;

  @Inject
  public UserResource(UserService userService) {
    this.userService = userService;
  }

  @POST
  @Path("register")
  public RestResponse<User> register(User user) {
    return new RestResponse<>(userService.register(user));
  }

  @GET
  @Path("login")
  public User login(@Auth User user) {
    return user; // TODO: mask fields
  }

  @GET
  @Path("secure")
  @AuthRule({USER})
  public String secure(@Context ContainerRequestContext crc) {
    User user = (User) crc.getProperty("USER");
    return user.getName();
  }
}
