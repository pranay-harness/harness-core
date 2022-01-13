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
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateNgTokenService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("/v2/delegate-token")
@Path("/v2/delegate-token")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Tag(name = "Delegate Token Resource", description = "Contains APIs related to Delegate NG Token management")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })

public class DelegateNgTokenResource {
  private final DelegateNgTokenService delegateTokenService;

  @Inject
  public DelegateNgTokenResource(DelegateNgTokenService delegateTokenService) {
    this.delegateTokenService = delegateTokenService;
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  @Operation(operationId = "createToken", summary = "Creates Delegate NG Token.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default", description = "A created Token.")
      })
  public RestResponse<DelegateTokenDetails>
  createToken(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                  "accountId") @NotNull String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = "Delegate Token name") @QueryParam("tokenName") @NotNull String tokenName) {
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    return new RestResponse<>(delegateTokenService.createToken(accountId, owner, tokenName));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  @Operation(operationId = "revokeDelegateToken", summary = "Revokes Delegate Ng Token.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "200 Ok response if everything successfully revoked token")
      })
  public RestResponse<Void>
  revokeToken(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                  "accountId") @NotNull String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = "Delegate Token name") @QueryParam("tokenName") @NotNull String tokenName) {
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    delegateTokenService.revokeDelegateToken(accountId, owner, tokenName);
    return new RestResponse<>();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Operation(operationId = "getDelegateTokens",
      summary = "Retrieves Delegate Ng Tokens by Account, Organization, Project, status and name.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "A list of Delegate Tokens")
      })
  public RestResponse<List<DelegateTokenDetails>>
  getDelegateTokens(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                        "accountId") @NotNull String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = "Status of Delegate Token (ACTIVE or REVOKED). "
              + "If left empty both active and revoked tokens will be retrieved") @QueryParam("status")
      DelegateTokenStatus status) {
    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);
    return new RestResponse<>(delegateTokenService.getDelegateTokens(accountId, owner, status));
  }

  @PUT
  @Path("default")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  @Operation(operationId = "upsertDefaultToken",
      summary = "Creates or a default Delegate Token for account, org and project. "
          + "If default token already exists its value will be re-generated.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "200 Ok response if successfully created default token")
      })
  public RestResponse<Void>
  upsertDefaultToken(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
                         "accountId") @NotNull String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ORG_KEY) String orgId,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @Parameter(description = "skipIfExists") @QueryParam("skipIfExists") Boolean skipIfExists) {
    delegateTokenService.upsertDefaultToken(
        accountId, DelegateEntityOwnerHelper.buildOwner(orgId, projectId), skipIfExists);
    return new RestResponse<>();
  }
}
