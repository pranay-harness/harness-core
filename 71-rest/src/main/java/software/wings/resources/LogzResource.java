package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.StateType;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 8/21/17.
 */
@Api(LogAnalysisResource.LOGZ_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.LOGZ_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class LogzResource implements LogAnalysisResource {
  @Inject private AnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getSampleLogRecord(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId) throws IOException {
    return new RestResponse<>(
        analysisService.getLogSample(accountId, analysisServerConfigId, null, StateType.LOGZ, -1));
  }

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_HOST_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getHostLogRecord(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId, @QueryParam("index") String index,
      @QueryParam("hostNameField") String hostNameField, @QueryParam("hostName") String hostName,
      @QueryParam("queryType") ElkQueryType queryType, @QueryParam("query") String query,
      @QueryParam("timeStampField") String timeStampField,
      @QueryParam("timeStampFieldFormat") String timeStampFieldFormat,
      @QueryParam("messageField") String messageField) {
    return new RestResponse<>(analysisService.getHostLogRecords(accountId, analysisServerConfigId, index, queryType,
        query, timeStampField, timeStampFieldFormat, messageField, hostNameField, hostName, StateType.LOGZ, false));
  }
}
