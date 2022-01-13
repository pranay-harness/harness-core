/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.rest.RestResponse;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.elk.ElkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rsingh on 08/04/17.
 * <p>
 * For api versioning see documentation of {@link NewRelicResource}.
 */
@Api(LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@Slf4j
public class ElkResource implements LogAnalysisResource {
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
      log.warn("Failed to get elk sample record " + result, ex);
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
      @QueryParam("timeStampFieldFormat") String timeStampFieldFormat,
      @QueryParam("messageField") String messageField) {
    return new RestResponse<>(analysisService.getHostLogRecords(accountId, analysisServerConfigId, index, queryType,
        query, timeStampField, timeStampFieldFormat, messageField, hostNameField, hostName, StateType.ELK));
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
      log.warn("Unable to get indices", ex);
    }
    return new RestResponse<>(null);
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
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(
      @QueryParam("accountId") String accountId, @Valid ElkSetupTestNodeData elkSetupTestNodeData) {
    log.info("Fetching log Records for verification for accountId : " + accountId
        + " and ElkSetupTestNodeData :" + elkSetupTestNodeData);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, elkSetupTestNodeData));
  }
}
