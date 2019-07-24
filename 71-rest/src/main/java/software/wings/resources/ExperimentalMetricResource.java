package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ExpAnalysisInfo;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.newrelic.ExperimentalMetricRecord;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.service.intfc.analysis.ExperimentalAnalysisService;
import software.wings.sm.StateType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.LEARNING_METRIC_EXP_URL)
@Path("/" + VerificationConstants.LEARNING_METRIC_EXP_URL)
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class ExperimentalMetricResource {
  @Inject private ExperimentalAnalysisService analysisService;
  @Inject private MetricDataAnalysisService metricDataAnalysisService;

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ExpAnalysisInfo>> getMetricExpAnalysisInfo(@QueryParam("offset") String offset) {
    PageRequest<ExperimentalMetricAnalysisRecord> pageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();
    return new RestResponse<>(analysisService.getMetricExpAnalysisInfoList(pageRequest));
  }

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<ExperimentalMetricRecord> getExperimentalAnalysisSummary(
      @QueryParam("stateExecutionId") String stateExecutionId, @QueryParam("stateType") StateType stateType,
      @QueryParam("expName") String expName) {
    return new RestResponse<>(
        analysisService.getExperimentalMetricAnalysisSummary(stateExecutionId, stateType, expName));
  }
}
