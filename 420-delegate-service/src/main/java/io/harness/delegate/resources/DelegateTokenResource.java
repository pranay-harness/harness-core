/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("/delegate-token")
@Path("/delegate-token")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenResource {
  private final DelegateTokenService delegateTokenService;

  @Inject
  public DelegateTokenResource(DelegateTokenService delegateTokenService) {
    this.delegateTokenService = delegateTokenService;
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<DelegateTokenDetails> createToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    return new RestResponse<>(delegateTokenService.createDelegateToken(accountId, tokenName));
  }

  @Path("{tokenName}")
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<String> getTokenValue(
      @QueryParam("accountId") @NotNull String accountId, @PathParam("tokenName") @NotNull String tokenName) {
    return new RestResponse<>(delegateTokenService.getTokenValue(accountId, tokenName));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<Void> revokeToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    delegateTokenService.revokeDelegateToken(accountId, tokenName);
    return new RestResponse<>();
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<Void> deleteToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    delegateTokenService.deleteDelegateToken(accountId, tokenName);
    return new RestResponse<>();
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<DelegateTokenDetails>> getDelegateTokens(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("status") DelegateTokenStatus status, @QueryParam("tokenName") String tokenName) {
    return new RestResponse<>(delegateTokenService.getDelegateTokens(accountId, status, tokenName));
  }
}
