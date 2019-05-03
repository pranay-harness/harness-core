package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.beans.User;
import software.wings.security.annotations.HarnessApiKeyAuth;
import software.wings.security.annotations.IdentityServiceAuth;
import software.wings.security.authentication.AuthenticationManager;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author mark.lu on 2019-04-29
 */
@Api("identity")
@Path("/identity")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@HarnessApiKeyAuth(clientTypes = ClientType.IDENTITY_SERVICE)
@Slf4j
public class IdentityServiceResource {
  private AuthenticationManager authenticationManager;

  @Inject
  public IdentityServiceResource(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @GET
  @Path("/user/login")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<User> loginUser(@QueryParam("email") String email) {
    return new RestResponse<>(authenticationManager.loginUserForIdentityService(urlDecode(email)));
  }

  private String urlDecode(String encoded) {
    String decoded = encoded;
    try {
      decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // Should not happen and ignore.
    }
    return decoded;
  }
}
