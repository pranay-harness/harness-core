/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.security.PermissionAttribute.Action.UPDATE;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.InfrastructureMappingService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("pcfresource")
@Path("pcfresource")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
@OwnedBy(CDP)
public class PCFResource {
  @Inject InfrastructureMappingService infrastructureMappingService;

  @POST
  @Path("create-route")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = UPDATE)
  public RestResponse<String> createRouteForPcf(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @QueryParam("computeProviderId") String computeProviderId, @QueryParam("org") String org,
      @QueryParam("space") String space, @QueryParam("host") String host, @QueryParam("domain") String domain,
      @QueryParam("path") String path, @QueryParam("port") String port,
      @QueryParam("useRandomPort") boolean useRandomPort, @QueryParam("tcpRoute") boolean tcpRoute) {
    return new RestResponse<>(infrastructureMappingService.createRoute(
        appId, computeProviderId, org, space, host, domain, path, tcpRoute, useRandomPort, port));
  }
}
