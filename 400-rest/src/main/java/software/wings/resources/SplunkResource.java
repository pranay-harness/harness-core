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
import software.wings.service.impl.splunk.SplunkSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.splunk.SplunkAnalysisService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Splunk Resource file.
 *
 * Created by Pranjal on 08/31/2018
 */
@Api(LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Path("/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@Slf4j
public class SplunkResource implements LogAnalysisResource {
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
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getLogRecords(
      @QueryParam("accountId") String accountId, @Valid SplunkSetupTestNodeData setupTestNodeData) {
    log.info("Fetching log Records for verification for accountId : " + accountId
        + " and SplunkSetupTestNodeData :" + setupTestNodeData);
    return new RestResponse<>(analysisService.getLogDataByHost(accountId, setupTestNodeData));
  }
}
