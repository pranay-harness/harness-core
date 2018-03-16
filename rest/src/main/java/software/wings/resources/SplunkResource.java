package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLFeedback;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class SplunkResource implements LogAnalysisResource {
  @Inject private AnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_ANALYSIS_SUMMARY_URL)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<LogMLAnalysisSummary> getLogAnalysisSummary(@QueryParam("accountId") String accountId,
      @QueryParam("applicationId") String applicationId, @QueryParam("stateExecutionId") String stateExecutionId)
      throws IOException {
    return new RestResponse<>(analysisService.getAnalysisSummary(stateExecutionId, applicationId, StateType.SPLUNKV2));
  }

  @POST
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> createUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    if (!isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("feedback id should not be set in POST call. to update feedback use PUT");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, StateType.SPLUNKV2));
  }

  @PUT
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> updateUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    if (isEmpty(feedback.getLogMLFeedbackId())) {
      throw new WingsException("logMlFeedBackId should be set for update");
    }
    return new RestResponse<>(analysisService.saveFeedback(feedback, StateType.SPLUNKV2));
  }

  @DELETE
  @Path(LogAnalysisResource.ANALYSIS_USER_FEEDBACK)
  @Timed
  @ExceptionMetered
  @Override
  public RestResponse<Boolean> deleteUserFeedback(@QueryParam("accountId") String accountId, LogMLFeedback feedback)
      throws IOException {
    return new RestResponse<>(analysisService.deleteFeedback(feedback));
  }
}
