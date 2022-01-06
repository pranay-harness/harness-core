/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.rest.RestResponse;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.datadog.DataDogSetupTestNodeData;
import software.wings.sm.states.DatadogState;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("datadog")
@Path("/datadog")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class DatadogResource {
  @Inject private ContinuousVerificationService verificationService;

  @GET
  @Path("/metric-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<DatadogState.Metric>> getAllMetricNames(@QueryParam("accountId") String accountId)
      throws IOException {
    return new RestResponse<>(DatadogState.metricNames());
  }

  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @QueryParam("serverConfigId") String serverConfigId,
      @Valid DataDogSetupTestNodeData dataDogSetupTestNodeData) {
    return new RestResponse<>(verificationService.getDataForNode(
        accountId, serverConfigId, dataDogSetupTestNodeData, dataDogSetupTestNodeData.getStateType()));
  }
}
