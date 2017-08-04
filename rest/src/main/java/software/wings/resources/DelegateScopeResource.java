package software.wings.resources;

import com.google.common.collect.ImmutableMap;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import freemarker.template.TemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.RestResponse;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DownloadTokenService;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Created by brett on 8/4/17
 */
@Api("delegate-scopes")
@Path("/delegate-scopes")
@Produces("application/json")
@AuthRule(ResourceType.DELEGATE_SCOPE)
public class DelegateScopeResource {
  private DelegateScopeService delegateScopeService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    return new RestResponse<Void>();
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
