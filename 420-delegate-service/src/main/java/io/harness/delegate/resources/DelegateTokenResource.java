package io.harness.delegate.resources;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateToken;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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

  @DelegateAuth
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateToken> createToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    return new RestResponse<>(delegateTokenService.createDelegateToken(accountId, tokenName));
  }

  @DelegateAuth
  @Path("{tokenName}")
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<String> getTokenValue(
      @QueryParam("accountId") @NotNull String accountId, @PathParam("tokenName") @NotNull String tokenName) {
    return new RestResponse<>(delegateTokenService.getTokenValue(accountId, tokenName));
  }

  @DelegateAuth
  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<Void> revokeToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    delegateTokenService.revokeDelegateToken(accountId, tokenName);
    return new RestResponse<>();
  }

  @DelegateAuth
  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Void> deleteToken(
      @QueryParam("accountId") @NotNull String accountId, @QueryParam("tokenName") @NotNull String tokenName) {
    delegateTokenService.deleteDelegateToken(accountId, tokenName);
    return new RestResponse<>();
  }
}
