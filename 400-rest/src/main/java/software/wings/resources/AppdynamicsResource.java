/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CV;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/14/17.
 *
 * For api versioning see documentation of {@link NewRelicResource}.
 *
 */
@Api("appdynamics")
@Path("/appdynamics")
@Produces("application/json")
@Scope(SETTING)
@OwnedBy(CV)
@TargetModule(HarnessModule._360_CG_MANAGER)
public class AppdynamicsResource {
  @Inject private AppdynamicsService appdynamicsService;

  @GET
  @Path("/applications")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NewRelicApplication>> getAllApplications(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") final String settingId) throws IOException {
    return new RestResponse<>(appdynamicsService.getApplications(settingId));
  }

  @GET
  @Path("/tiers")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<AppdynamicsTier>> getAllTiers(@QueryParam("accountId") String accountId,
      @QueryParam("settingId") final String settingId, @QueryParam("appdynamicsAppId") long appdynamicsAppId)
      throws IOException {
    return new RestResponse<>(appdynamicsService.getTiers(settingId, appdynamicsAppId));
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param appdynamicsSetupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId,
      @Valid AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData) {
    return new RestResponse<>(appdynamicsService.getMetricsWithDataForNode(appdynamicsSetupTestNodeData));
  }
}
