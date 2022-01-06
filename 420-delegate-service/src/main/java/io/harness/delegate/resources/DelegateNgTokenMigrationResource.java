/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.service.intfc.DelegateNgTokenService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("/v2/delegate-token-internal")
@Path("/v2/delegate-token-internal")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@NextGenManagerAuth
@OwnedBy(HarnessTeam.DEL)
public class DelegateNgTokenMigrationResource {
  private final DelegateNgTokenService delegateTokenService;

  @Inject
  public DelegateNgTokenMigrationResource(DelegateNgTokenService delegateTokenService) {
    this.delegateTokenService = delegateTokenService;
  }

  @PUT
  @Path("default")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  @Deprecated
  public RestResponse<Void> upsertDefaultToken(
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = "skipIfExists") @QueryParam("skipIfExists") Boolean skipIfExists) {
    delegateTokenService.upsertDefaultToken(
        accountIdentifier, DelegateEntityOwnerHelper.buildOwner(orgId, projectId), skipIfExists);
    return new RestResponse<>();
  }

  @GET
  @Path("default-for-orgs")
  @Timed
  @ExceptionMetered
  @Hidden
  @Deprecated
  public RestResponse<List<String>> getOrgsWithActiveDefaultDelegateToken(
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier) {
    return new RestResponse<>(delegateTokenService.getOrgsWithActiveDefaultDelegateTokens(accountIdentifier));
  }

  @GET
  @Path("default-for-projects")
  @Timed
  @ExceptionMetered
  @Hidden
  @Deprecated
  public RestResponse<List<String>> getProjectsWithActiveDefaultDelegateToken(
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier) {
    return new RestResponse<>(delegateTokenService.getProjectsWithActiveDefaultDelegateTokens(accountIdentifier));
  }
}
