package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.common.VerificationConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.service.intfc.analysis.AnalysisService;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(VerificationConstants.LEARNING_EXP_URL)
@Path("/" + VerificationConstants.LEARNING_EXP_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ExperimentalLogResource {
  @Inject private AnalysisService analysisService;

  @GET
  @Path(VerificationConstants.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<List<LogMLExpAnalysisInfo>> getLogExpAnalysisInfo(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(analysisService.getExpAnalysisInfoList());
  }
}
