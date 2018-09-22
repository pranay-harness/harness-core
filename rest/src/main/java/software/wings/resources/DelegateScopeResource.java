package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE_SCOPE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.DelegateScope;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateScopeService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by brett on 8/4/17
 */
@Api("delegate-scopes")
@Path("/delegate-scopes")
@Produces("application/json")
@Scope(DELEGATE_SCOPE)
@AuthRule(permissionType = LOGGED_IN)
public class DelegateScopeResource {
  private DelegateScopeService delegateScopeService;

  @Inject
  public DelegateScopeResource(DelegateScopeService delegateService) {
    this.delegateScopeService = delegateService;
  }

  @GET
  @ApiImplicitParams(
      { @ApiImplicitParam(name = "accountId", required = true, dataType = "string", paramType = "query") })
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<DelegateScope>>
  list(@BeanParam PageRequest<DelegateScope> pageRequest) {
    return new RestResponse<>(delegateScopeService.list(pageRequest));
  }

  @GET
  @Path("{delegateScopeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScope> get(@PathParam("delegateScopeId") @NotEmpty String delegateScopeId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateScopeService.get(accountId, delegateScopeId));
  }

  @DELETE
  @Path("{delegateScopeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(@PathParam("delegateScopeId") @NotEmpty String delegateScopeId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    delegateScopeService.delete(accountId, delegateScopeId);
    return new RestResponse<>();
  }

  @DelegateAuth
  @PUT
  @Path("{delegateScopeId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScope> update(@PathParam("delegateScopeId") @NotEmpty String delegateScopeId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateScope delegateScope) {
    delegateScope.setAccountId(accountId);
    delegateScope.setUuid(delegateScopeId);
    return new RestResponse<>(delegateScopeService.update(delegateScope));
  }

  @POST
  public RestResponse<DelegateScope> add(
      @QueryParam("accountId") @NotEmpty String accountId, DelegateScope delegateScope) {
    delegateScope.setAccountId(accountId);
    return new RestResponse<>(delegateScopeService.add(delegateScope));
  }
}
