package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by sriram_parthasarathy on 9/13/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class SumoResource implements LogAnalysisResource {
  @Inject private AnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.SUMO));
  }
}
