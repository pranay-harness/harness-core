package software.wings.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import software.wings.beans.User;

/**
 *  Users Resource class
 *
 *
 * @author Rishi
 *
 */
@Path("/users")
public class UserResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public User getDefaultUserInJOrderTypeN() {
    User user = new User();
    user.setFirstName("Default");
    user.setLastName("Wings User");
    return user;
  }
}
