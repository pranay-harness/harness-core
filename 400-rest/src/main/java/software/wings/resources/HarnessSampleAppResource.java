/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import io.harness.rest.RestResponse;

import software.wings.beans.Application;
import software.wings.beans.SampleAppStatus;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.HarnessSampleAppService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/sampleapps")
@Path("/sampleapps")
@Produces("application/json")
@Scope(APPLICATION)

public class HarnessSampleAppResource {
  private HarnessSampleAppService sampleAppService;

  @Inject
  public HarnessSampleAppResource(HarnessSampleAppService sampleAppService) {
    this.sampleAppService = sampleAppService;
  }

  @GET
  @Path("health")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<SampleAppStatus> getHealth(
      @QueryParam("accountId") String accountId, @QueryParam("deploymentType") String deploymentType) {
    return new RestResponse<>(sampleAppService.getSampleAppHealth(accountId, deploymentType));
  }

  @POST
  @Path("restore")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Application> restoreApp(
      @QueryParam("accountId") String accountId, @QueryParam("deploymentType") String deploymentType) {
    return new RestResponse<>(sampleAppService.restoreSampleApp(accountId, deploymentType));
  }
}
