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
import software.wings.service.impl.sumo.SumoLogicSetupTestNodedata;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Resource for SumoLogic.
 * Created by Pranjal on 08/21/2018
 */
@Api(LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SUMO_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@Slf4j
public class SumoLogicResource implements LogAnalysisResource {
  private static final String DEFAULT_DURATION = "10"; // in minutes

  @Inject private SumoLogicAnalysisService analysisService;

  @GET
  @Path(LogAnalysisResource.ANALYSIS_STATE_GET_SAMPLE_RECORD_URL)
  @Timed
  @ExceptionMetered
  public RestResponse<Object> getSampleLogRecord(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId,
      @DefaultValue(DEFAULT_DURATION) @QueryParam("durationInMinutes") int duration) {
    return new RestResponse<>(
        analysisService.getLogSample(accountId, analysisServerConfigId, null, StateType.SUMO, duration));
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
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(@QueryParam("accountId") String accountId,
      @QueryParam("serverConfigId") String analysisServerConfigId,
      @Valid SumoLogicSetupTestNodedata sumoLogicSetupTestNodedata) {
    log.info("Fetching log Records for verification for accountId : " + accountId
        + " and SumoLogicSetupTestNodedata :" + sumoLogicSetupTestNodedata);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, sumoLogicSetupTestNodedata));
  }
}
