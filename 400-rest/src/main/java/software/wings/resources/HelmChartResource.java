/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.appmanifest.HelmChart;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("helm-charts")
@Path("/helm-charts")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(SERVICE)
@OwnedBy(HarnessTeam.CDC)
public class HelmChartResource {
  @Inject private HelmChartService helmChartService;

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<HelmChart> save(
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId, @NotNull HelmChart helmChart) {
    helmChart.setServiceId(serviceId);
    helmChart.setAppId(appId);
    return new RestResponse<>(helmChartService.create(helmChart));
  }

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  public RestResponse<List<HelmChart>> list(@QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @QueryParam("applicationManifestId") String applicationManifestId) {
    return new RestResponse<>(helmChartService.fetchChartsFromRepo(accountId, appId, serviceId, applicationManifestId));
  }
}
