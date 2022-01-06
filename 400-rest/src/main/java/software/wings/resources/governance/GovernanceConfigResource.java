/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.governance;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES;

import io.harness.governance.DeploymentFreezeInfo;
import io.harness.rest.RestResponse;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Deployment Governance Resource class.
 * Note: The path was named compliance-config since Governance was originally called Compliance.
 * Needs a UI change.
 *
 * @author rktummala on 02/04/19
 */
@Api("compliance-config")
@Path("/compliance-config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
public class GovernanceConfigResource {
  private GovernanceConfigService governanceConfigService;

  /**
   * Instantiates a new Governance resource.
   *
   * @param governanceConfigService Governance Service
   */
  @Inject
  public GovernanceConfigResource(GovernanceConfigService governanceConfigService) {
    this.governanceConfigService = governanceConfigService;
  }

  /**
   * Gets the Deployment Governance config for the given account id.
   *
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<GovernanceConfig> get(@PathParam("accountId") String accountId) {
    return new RestResponse<>(governanceConfigService.get(accountId));
  }

  /**
   * Update the Governance config.
   *
   * @param accountId   the account id
   * @return the rest response
   */
  @PUT
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DEPLOYMENT_FREEZES)
  public RestResponse<GovernanceConfig> update(
      @PathParam("accountId") String accountId, GovernanceConfig governanceConfig) {
    governanceConfig.setAccountId(accountId);
    return new RestResponse<>(governanceConfigService.upsert(accountId, governanceConfig));
  }

  @GET
  @Path("deploymentfreeze/info/{accountId}")
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<DeploymentFreezeInfo> getDeploymentFreezeInfo(@PathParam("accountId") String accountId) {
    return new RestResponse<>(governanceConfigService.getDeploymentFreezeInfo(accountId));
  }
}
