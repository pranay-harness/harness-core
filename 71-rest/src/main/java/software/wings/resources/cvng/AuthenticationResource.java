package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_NEXTGEN_RESOURCE_PREFIX;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;
import retrofit2.http.Query;
import software.wings.security.AuthenticationFilter;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;

@Api(CV_NEXTGEN_RESOURCE_PREFIX + "/auth")
@Path(CV_NEXTGEN_RESOURCE_PREFIX + "/auth")
@Produces("application/json")
public class AuthenticationResource {
  @Inject AuthenticationFilter authenticationFilter;

  @GET
  @Path("/validate-token")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> authenticateToken(
      @Query("containerRequestContext") ContainerRequestContext containerRequestContext) throws IOException {
    authenticationFilter.filter(containerRequestContext);
    return new RestResponse<>(true);
  }
}
