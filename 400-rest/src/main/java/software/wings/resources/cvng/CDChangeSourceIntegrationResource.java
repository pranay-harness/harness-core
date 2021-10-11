package software.wings.resources.cvng;

import static io.harness.cvng.core.services.CVNextGenConstants.CD_CURRENT_GEN_CHANGE_EVENTS_PATH;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.change.HarnessCDCurrentGenEventMetadata;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.service.intfc.cvng.CDChangeSourceIntegrationService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api(CD_CURRENT_GEN_CHANGE_EVENTS_PATH)
@Path(CD_CURRENT_GEN_CHANGE_EVENTS_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@OwnedBy(HarnessTeam.CV)
@LearningEngineAuth
@ExposeInternalException(withStackTrace = true)
public class CDChangeSourceIntegrationResource {
  @Inject private CDChangeSourceIntegrationService cdChangeSourceIntegrationService;

  @GET
  public RestResponse<List<HarnessCDCurrentGenEventMetadata>> getChangeEvents(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("environmentId") String environmentId, @QueryParam("timestamp") Long timestamp) {
    return new RestResponse<>(cdChangeSourceIntegrationService.getCurrentGenEventsBetween(
        accountId, appId, serviceId, environmentId, Instant.ofEpochMilli(timestamp)));
  }
}
