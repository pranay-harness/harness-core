package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.sumo.SumoLogicSetupTestNodedata;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;
import software.wings.sm.StateType;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Resource for SumoLogic.
 * Created by Pranjal on 08/21/2018
 */
@Api(LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class SumoLogicResource implements LogAnalysisResource {
  private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

  @Inject private SumoLogicAnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getSampleLogRecord(
      @QueryParam("accountId") String accountId, @QueryParam("serverConfigId") String analysisServerConfigId) {
    return new RestResponse<>(analysisService.getLogSample(accountId, analysisServerConfigId, null, StateType.SUMO));
  }

  /**
   * API to get log Records based on provided node data.
   *
   * @param accountId : account id.
   * @param sumoLogicSetupTestNodedata : configuration details for test node.
   * @return {@link VerificationNodeDataSetupResponse}
   */
  @POST
  @Path(LogAnalysisResource.TEST_NODE_DATA)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId,
      @Valid SumoLogicSetupTestNodedata sumoLogicSetupTestNodedata) {
    logger.info("Fetching log Records for verification for accountId : " + accountId
        + " and SumoLogicSetupTestNodedata :" + sumoLogicSetupTestNodedata);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, sumoLogicSetupTestNodedata));
  }
}
