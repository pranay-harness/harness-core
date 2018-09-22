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
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.splunk.SplunkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.splunk.SplunkAnalysisService;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Splunk Resource file.
 *
 * Created by Pranjal on 08/31/2018
 */
@Api(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class SplunkResource implements LogAnalysisResource {
  private static final Logger logger = LoggerFactory.getLogger(SplunkResource.class);

  @Inject private SplunkAnalysisService analysisService;

  /**
   * API to get log Records based on provided node data.
   *
   * @param accountId : account id.
   * @param setupTestNodeData : configuration details for test node.
   * @return {@link VerificationNodeDataSetupResponse}
   */
  @POST
  @Path(LogAnalysisResource.TEST_NODE_DATA)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(
      @QueryParam("accountId") String accountId, @Valid SplunkSetupTestNodeData setupTestNodeData) {
    logger.info("Fetching log Records for verification for accountId : " + accountId
        + " and SplunkSetupTestNodeData :" + setupTestNodeData);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, setupTestNodeData));
  }
}
