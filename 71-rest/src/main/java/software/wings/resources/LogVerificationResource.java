package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.VerificationConstants;
import software.wings.security.annotations.LearningEngineAuth;
import software.wings.service.impl.analysis.LogAnalysisResponse;
import software.wings.service.intfc.analysis.LogVerificationService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by Pranjal on 04/10/2019
 */
@Api("log-verification")
@Path("/log-verification")
@Produces("application/json")
public class LogVerificationResource {
  private static final Logger logger = LoggerFactory.getLogger(LogVerificationResource.class);
  @Inject private LogVerificationService logVerificationService;

  @POST
  @Path(VerificationConstants.NOTIFY_LOG_STATE)
  @Timed
  @LearningEngineAuth
  public RestResponse<Boolean> sendNotifyForLogAnalysis(
      @QueryParam("correlationId") String correlationId, LogAnalysisResponse response) {
    logger.info("Sending notification for log Analysis with correlationId {}", correlationId);
    return new RestResponse<>(logVerificationService.sendNotifyForLogAnalysis(correlationId, response));
  }
}
