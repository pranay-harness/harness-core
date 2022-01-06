/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE_SCOPE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateTaskSelectorMapService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("delegate-task-selector-map")
@Path("/delegate-task-selector-map")
@Produces("application/json")
@Scope(DELEGATE_SCOPE)
@AuthRule(permissionType = LOGGED_IN)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
// TODO: we should use this auth rule: @AuthRule(permissionType = MANAGE_TASK_SELECTORS)
@OwnedBy(DEL)
public class DelegateTaskSelectorMapResource {
  private DelegateTaskSelectorMapService selectorMapService;

  @Inject
  public DelegateTaskSelectorMapResource(DelegateTaskSelectorMapService selectorMapService) {
    this.selectorMapService = selectorMapService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<TaskSelectorMap>> list(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(selectorMapService.list(accountId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<TaskSelectorMap> add(
      @QueryParam("accountId") @NotEmpty String accountId, TaskSelectorMap taskSelectorMap) {
    taskSelectorMap.setAccountId(accountId);
    return new RestResponse<>(selectorMapService.add(taskSelectorMap));
  }

  @PUT
  @Path("/{taskSelectorMapId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<TaskSelectorMap> update(@PathParam("taskSelectorMapId") @NotEmpty String taskSelectorMapId,
      @QueryParam("accountId") @NotEmpty String accountId, TaskSelectorMap taskSelectorMap) {
    taskSelectorMap.setAccountId(accountId);
    taskSelectorMap.setUuid(taskSelectorMapId);
    return new RestResponse<>(selectorMapService.update(taskSelectorMap));
  }

  @POST
  @Path("/{taskSelectorMapId}/task-selectors")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<TaskSelectorMap> addTaskSelector(
      @PathParam("taskSelectorMapId") @NotEmpty String taskSelectorMapId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("selector") @NotEmpty String taskSelector) {
    return new RestResponse<>(selectorMapService.addTaskSelector(accountId, taskSelectorMapId, taskSelector));
  }

  @DELETE
  @Path("/{taskSelectorMapId}/task-selectors")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<TaskSelectorMap> deleteTaskSelector(
      @PathParam("taskSelectorMapId") @NotEmpty String taskSelectorMapId,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("selector") @NotEmpty String taskSelector) {
    return new RestResponse<>(selectorMapService.removeTaskSelector(accountId, taskSelectorMapId, taskSelector));
  }
}
