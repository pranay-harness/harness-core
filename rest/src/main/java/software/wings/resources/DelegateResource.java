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
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.PublicApi;
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
 * Created by peeyushaggarwal on 11/28/16.
 */
@Api("delegates")
@Path("/delegates")
@Produces("application/json")
@AuthRule(ResourceType.DELEGATE)
public class DelegateResource {
  private DelegateService delegateService;
  private DownloadTokenService downloadTokenService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  public DelegateResource(DelegateService delegateService, DownloadTokenService downloadTokenService) {
    this.delegateService = delegateService;
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
  @Path("{deletgateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> get(
      @PathParam("deletgateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.get(accountId, delegateId));
  }

  @DELETE
  @Path("{deletgateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(
      @PathParam("deletgateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.delete(accountId, delegateId);
    return new RestResponse<Void>();
  }

  @DelegateAuth
  @PUT
  @Path("{deletgateId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> update(@PathParam("deletgateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    delegate.setUuid(delegateId);
    return new RestResponse<>(delegateService.update(delegate));
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
    logger.info("Delegate registration took {} in ms", (System.currentTimeMillis() - startTime));
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
        .header("Content-Disposition", "attachment; filename=wings-bot.zip")
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
  @GET
  @Path("{deletgateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("deletgateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException, TemplateException {
    return new RestResponse<>(delegateService.checkForUpgrade(
        accountId, delegateId, version, request.getServerName() + ":" + request.getServerPort()));
  }
}
