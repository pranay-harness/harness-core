package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.security.annotations.DelegateAuth;
import io.harness.service.intfc.DelegateTaskService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("agent/dms")
@Path("agent/dms")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateTaskResource {
  private DelegateTaskService delegateTaskService;

  @Inject
  public DelegateTaskResource(DelegateTaskService delegateTaskService) {
    this.delegateTaskService = delegateTaskService;
  }

  @DelegateAuth
  @POST
  @Path("{taskId}/delegates/{delegateId}")
  @Consumes("application/x-kryo")
  @Timed
  @ExceptionMetered
  public void updateTaskResponse(@PathParam("delegateId") String delegateId, @PathParam("taskId") String taskId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateTaskResponse delegateTaskResponse) {
    try (AutoLogContext ignore1 = new TaskLogContext(taskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore3 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      try {
        delegateTaskService.processDelegateResponse(accountId, delegateId, taskId, delegateTaskResponse);
      } catch (Exception exception) {
        log.error("Error during update task response. delegateId: {}, taskId: {}, delegateTaskResponse: {}.",
            delegateId, taskId, delegateTaskResponse, exception);
      }
    }
  }
}
