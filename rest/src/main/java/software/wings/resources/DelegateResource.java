package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

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
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.RestResponse;
import software.wings.common.Constants;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
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
import java.util.List;
import java.util.Map;
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
 * Created by peeyushaggarwal on 11/28/16.
 */
@Api("delegates")
@Path("/delegates")
@Produces("application/json")
@AuthRule(ResourceType.DELEGATE)
public class DelegateResource {
  private DelegateService delegateService;
  private DelegateScopeService delegateScopeService;
  private DownloadTokenService downloadTokenService;
  private static final Logger logger = LoggerFactory.getLogger(DelegateResource.class);

  @Inject
  public DelegateResource(DelegateService delegateService, DelegateScopeService delegateScopeService,
      DownloadTokenService downloadTokenService) {
    this.delegateService = delegateService;
    this.delegateScopeService = delegateScopeService;
    this.downloadTokenService = downloadTokenService;
  }

  @GET
  @ApiImplicitParams(
      { @ApiImplicitParam(name = "accountId", required = true, dataType = "string", paramType = "query") })
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Delegate>>
  list(@BeanParam PageRequest<Delegate> pageRequest) {
    return new RestResponse<>(delegateService.list(pageRequest));
  }

  @GET
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> get(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.get(accountId, delegateId));
  }

  @GET
  @Path("latest")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getLatestDelegateVersion());
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> update(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    delegate.setUuid(delegateId);
    return new RestResponse<>(delegateService.update(delegate));
  }

  @DELETE
  @Path("{delegateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.delete(accountId, delegateId);
    return new RestResponse<>();
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}/scopes")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> updateScopes(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateScopes delegateScopes) {
    Delegate delegate = delegateService.get(accountId, delegateId);
    if (delegateScopes == null) {
      delegate.setIncludeScopes(null);
      delegate.setExcludeScopes(null);
    } else {
      if (isNotEmpty(delegateScopes.getIncludeScopeIds())) {
        delegate.setIncludeScopes(delegateScopes.getIncludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .collect(toList()));
      } else {
        delegate.setIncludeScopes(null);
      }
      if (isNotEmpty(delegateScopes.getExcludeScopeIds())) {
        delegate.setExcludeScopes(delegateScopes.getExcludeScopeIds()
                                      .stream()
                                      .map(s -> delegateScopeService.get(accountId, s))
                                      .collect(toList()));
      } else {
        delegate.setExcludeScopes(null);
      }
    }
    return new RestResponse<>(delegateService.updateScopes(delegate));
  }

  private static class DelegateScopes {
    private List<String> includeScopeIds;
    private List<String> excludeScopeIds;

    public List<String> getIncludeScopeIds() {
      return includeScopeIds;
    }

    public void setIncludeScopeIds(List<String> includeScopeIds) {
      this.includeScopeIds = includeScopeIds;
    }

    public List<String> getExcludeScopeIds() {
      return excludeScopeIds;
    }

    public void setExcludeScopeIds(List<String> excludeScopeIds) {
      this.excludeScopeIds = excludeScopeIds;
    }
  }

  @DelegateAuth
  @POST
  @Path("register")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> register(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    if (delegate.getAppId() == null) {
      delegate.setAppId(Base.GLOBAL_APP_ID);
    }
    long startTime = System.currentTimeMillis();
    Delegate register = delegateService.register(delegate);
    logger.info("Delegate registration took {} in ms", System.currentTimeMillis() - startTime);
    return new RestResponse<>(register);
  }

  @POST
  public RestResponse<Delegate> add(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    return new RestResponse<>(delegateService.add(delegate));
  }

  @Produces("application/x-kryo")
  @DelegateAuth
  @GET
  @Path("{delegateId}/tasks")
  public RestResponse<PageResponse<DelegateTask>> getTasks(
      @PathParam("delegateId") String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.getDelegateTasks(accountId, delegateId));
  }

  @GET
  @Path("downloadUrl")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> downloadUrl(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException, TemplateException {
    return new RestResponse<>(ImmutableMap.of("downloadUrl",
        request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
            + request.getRequestURI().replace("downloadUrl", "download") + "?accountId=" + accountId
            + "&token=" + downloadTokenService.createDownloadToken("delegate." + accountId)));
  }

  @PublicApi
  @GET
  @Path("download")
  @Timed
  @ExceptionMetered
  public Response download(@Context HttpServletRequest request, @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("token") @NotEmpty String token) throws IOException, TemplateException {
    downloadTokenService.validateDownloadToken("delegate." + accountId, token);
    File delegateFile = delegateService.download(request.getServerName() + ":" + request.getServerPort(), accountId);
    return Response.ok(delegateFile)
        .header("Content-Transfer-Encoding", "binary")
        .type("application/zip; charset=binary")
        .header("Content-Disposition", "attachment; filename=" + Constants.DELEGATE_DIR + ".zip")
        .build();
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes("application/x-kryo")
  @Timed
  @ExceptionMetered
  public void updateTaskResponse(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTaskResponse delegateTaskResponse) {
    delegateService.processDelegateResponse(delegateTaskResponse);
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/acquire")
  @Timed
  @ExceptionMetered
  public DelegateTask acquireDelegateTask(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId) {
    return delegateService.acquireDelegateTask(accountId, delegateId, taskId);
  }

  @DelegateAuth
  @POST
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/report")
  @Timed
  @ExceptionMetered
  public DelegateTask reportConnectionResults(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      List<DelegateConnectionResult> results) {
    return delegateService.reportConnectionResults(accountId, delegateId, taskId, results);
  }

  @DelegateAuth
  @GET
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/proceed")
  @Timed
  @ExceptionMetered
  public DelegateTask shouldProceedAnyway(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId) {
    return delegateService.shouldProceedAnyway(accountId, delegateId, taskId);
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/start")
  @Timed
  @ExceptionMetered
  public DelegateTask startDelegateTask(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    return delegateService.startDelegateTask(accountId, delegateId, taskId);
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}/clear-cache")
  @Timed
  @ExceptionMetered
  public void clearCache(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.clearCache(delegateId);
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException, TemplateException {
    return new RestResponse<>(delegateService.checkForUpgrade(
        accountId, delegateId, version, request.getServerName() + ":" + request.getServerPort()));
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/task-events")
  @Timed
  @ExceptionMetered
  public List<DelegateTaskEvent> getDelegateTaskEvents(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("syncOnly") boolean syncOnly) {
    return delegateService.getDelegateTaskEvents(accountId, delegateId, syncOnly);
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/heartbeat")
  @Timed
  @ExceptionMetered
  public Delegate updateDelegateHB(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return delegateService.updateHeartbeat(accountId, delegateId);
  }
}
