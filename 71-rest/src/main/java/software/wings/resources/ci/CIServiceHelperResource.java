package software.wings.resources.ci;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.Timed;
import io.harness.delegate.beans.ResponseData;
import io.harness.rest.RestResponse;
import software.wings.common.CICommonEndpointConstants;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.ci.CIDelegateTaskHelperService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *  This is temporary resource class for accepting CI tasks.
 *  Temporarily we are using Learning engine token
 */

@Path("/ci")
@Produces("application/json")
public class CIServiceHelperResource {
  @Inject private CIDelegateTaskHelperService ciDelegateTaskHelperService;

  @POST
  @Path(CICommonEndpointConstants.CI_SETUP_ENDPOINT)
  @Timed
  @LearningEngineAuth
  public RestResponse<ResponseData> setBuildEnv() {
    return new RestResponse<>(ciDelegateTaskHelperService.setBuildEnv(null));
  }
}
