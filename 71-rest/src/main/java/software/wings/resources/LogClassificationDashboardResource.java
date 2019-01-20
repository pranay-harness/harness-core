package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.beans.RestResponse;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;
import software.wings.service.intfc.analysis.LogLabelingService;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.LOG_CLASSIFY_URL)
@Path("/" + VerificationConstants.LOG_CLASSIFY_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class LogClassificationDashboardResource {
  @Inject LogLabelingService logLabelingService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogDataRecord>> getLogRecordsToClassify(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(logLabelingService.getLogRecordsToClassify(accountId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveLabeledRecord(@QueryParam("accountId") final String accountId,
      @QueryParam("labels") List<LogLabel> labels, @Body Object params) {
    logLabelingService.saveClassifiedLogRecord(null, labels, accountId, params);
    return new RestResponse<>(true);
  }

  @GET
  @Path(VerificationConstants.GET_CLASSIFY_LABELS_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogLabel>> getLabels(@QueryParam("accountId") String accountId) throws IOException {
    return new RestResponse<>(logLabelingService.getLabels());
  }
}
