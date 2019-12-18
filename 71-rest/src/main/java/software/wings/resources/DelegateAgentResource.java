package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import freemarker.template.TemplateException;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegatePackage;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateTaskEvent;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.ratelimit.DelegateRequestRateLimiter;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

/**
 * Created by rohitkarelia on 11/18/19.
 */
@Api("/agent/delegates")
@Path("/agent/delegates")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateAgentResource {
  private DelegateService delegateService;
  private MainConfiguration mainConfiguration;
  private AccountService accountService;
  private WingsPersistence wingsPersistence;
  private DelegateRequestRateLimiter delegateRequestRateLimiter;

  @Inject
  public DelegateAgentResource(DelegateService delegateService, MainConfiguration mainConfiguration,
      AccountService accountService, WingsPersistence wingsPersistence,
      DelegateRequestRateLimiter delegateRequestRateLimiter) {
    this.delegateService = delegateService;
    this.mainConfiguration = mainConfiguration;
    this.accountService = accountService;
    this.wingsPersistence = wingsPersistence;
    this.delegateRequestRateLimiter = delegateRequestRateLimiter;
  }

  @DelegateAuth
  @GET
  @Path("configuration")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateConfiguration> getDelegateConfiguration(
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(accountService.getDelegateConfiguration(accountId));
    }
  }

  @DelegateAuth
  @POST
  @Path("register")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> register(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      delegate.setAccountId(accountId);
      long startTime = System.currentTimeMillis();
      Delegate register = delegateService.register(delegate);
      logger.info("Delegate registration took {} in ms", System.currentTimeMillis() - startTime);
      return new RestResponse<>(register);
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/profile")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfileParams> checkForProfile(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, @QueryParam("profileId") String profileId,
      @QueryParam("lastUpdatedAt") Long lastUpdatedAt) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      DelegateProfileParams profileParams =
          delegateService.checkForProfile(accountId, delegateId, profileId, lastUpdatedAt);
      return new RestResponse<>(profileParams);
    }
  }

  @DelegateAuth
  @POST
  @Path("connectionHeartbeat/{delegateId}")
  @Timed
  @ExceptionMetered
  public void connectionHeartbeat(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("delegateId") String delegateId, DelegateConnectionHeartbeat connectionHeartbeat) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.doConnectionHeartbeat(accountId, delegateId, connectionHeartbeat);
    }
  }

  @POST
  public RestResponse<Delegate> add(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      delegate.setAccountId(accountId);
      return new RestResponse<>(delegateService.add(delegate));
    }
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/tasks/{taskId}")
  @Consumes("application/x-kryo")
  @Timed
  @ExceptionMetered
  public void updateTaskResponse(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTaskResponse delegateTaskResponse) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.processDelegateResponse(accountId, delegateId, taskId, delegateTaskResponse);
    }
  }

  @DelegateAuth
  @PUT
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/acquire")
  @Timed
  @ExceptionMetered
  public DelegatePackage acquireDelegateTask(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      if (delegateRequestRateLimiter.isOverRateLimit(accountId, delegateId)) {
        return null;
      }
      return delegateService.acquireDelegateTask(accountId, delegateId, taskId);
    }
  }

  @DelegateAuth
  @POST
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/report")
  @Timed
  @ExceptionMetered
  public DelegatePackage reportConnectionResults(@PathParam("delegateId") String delegateId,
      @PathParam("taskId") String taskId, @QueryParam("accountId") @NotEmpty String accountId,
      List<DelegateConnectionResult> results) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return delegateService.reportConnectionResults(accountId, delegateId, taskId, results);
    }
  }

  @DelegateAuth
  @GET
  @Produces("application/x-kryo")
  @Path("{delegateId}/tasks/{taskId}/fail")
  @Timed
  @ExceptionMetered
  public void failIfAllDelegatesFailed(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.failIfAllDelegatesFailed(accountId, delegateId, taskId);
    }
  }

  @DelegateAuth
  @PUT
  @Path("{delegateId}/clear-cache")
  @Timed
  @ExceptionMetered
  public void clearCache(
      @PathParam("delegateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      delegateService.clearCache(accountId, delegateId);
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/upgrade")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> checkForUpgrade(@Context HttpServletRequest request,
      @HeaderParam("Version") String version, @PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId) throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          delegateService.getDelegateScripts(accountId, version, getManagerUrl(request), getVerificationUrl(request)));
    }
  }

  @DelegateAuth
  @GET
  @Path("delegateScripts")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateScripts> getDelegateScripts(@Context HttpServletRequest request,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("delegateVersion") @NotEmpty String delegateVersion) throws IOException, TemplateException {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(delegateService.getDelegateScripts(
          accountId, delegateVersion, getManagerUrl(request), getVerificationUrl(request)));
    }
  }

  @DelegateAuth
  @GET
  @Path("{delegateId}/task-events")
  @Timed
  @ExceptionMetered
  public List<DelegateTaskEvent> getDelegateTaskEvents(@PathParam("delegateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("syncOnly") boolean syncOnly) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      return delegateService.getDelegateTaskEvents(accountId, delegateId, syncOnly);
    }
  }

  @DelegateAuth
  @POST
  @Path("heartbeat-with-polling")
  @Timed
  @ExceptionMetered
  public RestResponse<Delegate> updateDelegateHB(
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegate.getUuid(), OVERRIDE_ERROR)) {
      // delegate.isPolllingModeEnabled() will be true here.
      if ("ECS".equals(delegate.getDelegateType())) {
        Delegate registeredDelegate = delegateService.handleEcsDelegateRequest(delegate);
        return new RestResponse<>(registeredDelegate);
      } else {
        return new RestResponse<>(delegateService.updateHeartbeatForDelegateWithPollingEnabled(delegate));
      }
    }
  }

  @DelegateAuth
  @POST
  @Path("{delegateId}/state-executions")
  @Timed
  @ExceptionMetered
  public void saveApiCallLogs(@PathParam("delegateId") String delegateId, @QueryParam("accountId") String accountId,
      List<ThirdPartyApiCallLog> logs) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      wingsPersistence.save(logs);
    }
  }

  private String getManagerUrl(HttpServletRequest request) {
    String apiUrl = mainConfiguration.getApiUrl();
    return !StringUtils.isEmpty(apiUrl)
        ? apiUrl
        : request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }

  private String getVerificationUrl(HttpServletRequest request) {
    return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
  }
}
