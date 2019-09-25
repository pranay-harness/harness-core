package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.resources.UserResource.UpdatePasswordRequest;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.signup.SignupException;
import software.wings.service.intfc.signup.SignupService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("signup")
@Path("/signup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.USER)
@AuthRule(permissionType = LOGGED_IN)
@Slf4j
public class SignupResource {
  @Inject SignupService signupService;

  /**
   *  Start the trial registration with email and user info. (Doesn't contains password)
   *  A verification email will be sent to the specified email address asking him to setup his password.
   *  On successful verification, it creates the account and registers the user.
   *
   * @param userInvite user invite with email and user info
   */
  @PublicApi
  @POST
  @Path("{source}")
  public RestResponse<Boolean> signup(UserInvite userInvite, @PathParam("source") String source) {
    try {
      return new RestResponse<>(signupService.signup(userInvite, source));
    } catch (SignupException ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("Failed to complete signup", ex);
      throw new SignupException("Failed to signup. Please contact harness support");
    }
  }

  @PublicApi
  @POST
  @Path("complete/{token}")
  public RestResponse<User> completeSignup(
      UpdatePasswordRequest passwordRequest, @NotEmpty @PathParam("token") String secretToken) {
    try {
      return new RestResponse<>(signupService.completeSignup(passwordRequest, secretToken));
    } catch (SignupException ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("Failed to complete signup", ex);
      throw new SignupException("Failed to signup. Please contact harness support");
    }
  }
}
