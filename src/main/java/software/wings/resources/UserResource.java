package software.wings.resources;

import io.dropwizard.auth.Auth;
import software.wings.app.WingsBootstrap;
import software.wings.beans.Base;
import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.annotations.AuthRule;
import software.wings.service.UserService;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static software.wings.security.PermissionAttr.USER;

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
  private UserService userService = WingsBootstrap.lookup(UserService.class);

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
