package software.wings.resources;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.elk.ElkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 08/04/17.
 * <p>
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class ElkResource implements LogAnalysisResource {
  private static final Logger logger = LoggerFactory.getLogger(ElkResource.class);

  @Inject private ElkAnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getSampleLogRecord(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId, @QueryParam("index") String index)
      throws IOException {
    Map<String, Map<String, List<Map>>> result = null;
    try {
      result = (Map<String, Map<String, List<Map>>>) analysisService.getLogSample(
          accountId, analysisServerConfigId, index, StateType.ELK, -1);
      return new RestResponse<>(result.get("hits").get("hits").get(0).get("_source"));
    } catch (Exception ex) {
      logger.warn("Failed to get elk sample record " + result, ex);
    }
    return new RestResponse<>();
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
      @QueryParam("timeStampFieldFormat") String timeStampFieldFormat, @QueryParam("messageField") String messageField,
      @QueryParam("isFormatedQuery") boolean isFormatedQuery) {
    return new RestResponse<>(
        analysisService.getHostLogRecords(accountId, analysisServerConfigId, index, queryType, query, timeStampField,
            timeStampFieldFormat, messageField, hostNameField, hostName, StateType.ELK, isFormatedQuery));
  }

  @GET
  @Path(LogAnalysisResource.ELK_GET_INDICES_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, ElkIndexTemplate>> getIndices(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId) throws IOException {
    try {
      return new RestResponse<>(analysisService.getIndices(accountId, analysisServerConfigId));
    } catch (Exception ex) {
      logger.warn("Unable to get indices", ex);
    }
    return new RestResponse<>(null);
  }

  @GET
  @Path(LogAnalysisResource.VALIDATE_QUERY)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> validateQuery(
      @QueryParam("accountId") String accountId, @QueryParam("query") String query) throws IOException {
    try {
      ElkLogFetchRequest.builder()
          .query(query)
          .indices("logstash-*")
          .hostnameField("beat.hostname")
          .messageField("message")
          .timestampField("@timestamp")
          .hosts(Sets.newHashSet("ip-172-31-8-144", "ip-172-31-12-79", "ip-172-31-13-153"))
          .startTime(1518724315175L - TimeUnit.MINUTES.toMillis(1))
          .endTime(1518724315175L)
          .queryType(ElkQueryType.TERM)
          .build()
          .toElasticSearchJsonObject();
      return new RestResponse<>(true);
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR).addParam("reason", Misc.getMessage(ex));
    }
  }

  /**
   * API to get log Records based on provided node data.
   *
   * @param accountId : account id.
   * @param elkSetupTestNodeData : configuration details for test node.
   * @return {@link VerificationNodeDataSetupResponse}
   */
  @POST
  @Path(LogAnalysisResource.TEST_NODE_DATA)
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(
      @QueryParam("accountId") String accountId, @Valid ElkSetupTestNodeData elkSetupTestNodeData) {
    logger.info("Fetching log Records for verification for accountId : " + accountId
        + " and ElkSetupTestNodeData :" + elkSetupTestNodeData);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, elkSetupTestNodeData));
  }
}
