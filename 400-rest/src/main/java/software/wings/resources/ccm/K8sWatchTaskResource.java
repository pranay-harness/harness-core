/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.ccm;

import io.harness.annotations.ExposeInternalException;
import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.ccm.ngperpetualtask.service.K8sWatchTaskService;
import io.harness.ccm.perpetualtask.K8sWatchTaskResourceClient;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Service;

@Api(K8sWatchTaskResourceClient.K8S_WATCH_TASK_RESOURCE_ENDPOINT)
@Path(K8sWatchTaskResourceClient.K8S_WATCH_TASK_RESOURCE_ENDPOINT)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Slf4j
@Service
@NextGenManagerAuth
@ExposeInternalException(withStackTrace = true)
public class K8sWatchTaskResource {
  @Inject K8sWatchTaskService k8sWatchTaskService;

  @POST
  @Timed
  @ExceptionMetered
  @Path("/create")
  @ApiOperation(value = "create", nickname = "create perpetual task")
  public ResponseDTO<String> create(@NotEmpty @QueryParam(K8sWatchTaskResourceClient.ACCOUNT_ID) String accountId,
      @NotNull K8sEventCollectionBundle k8sEventCollectionBundle) {
    final String pTaskId = k8sWatchTaskService.create(accountId, k8sEventCollectionBundle);
    return ResponseDTO.newResponse(pTaskId);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/reset")
  @ApiOperation(value = "reset", nickname = "reset perpetual task")
  public ResponseDTO<Boolean> reset(@NotEmpty @QueryParam(K8sWatchTaskResourceClient.ACCOUNT_ID) String accountId,
      @NotNull @QueryParam(K8sWatchTaskResourceClient.TASK_ID) String taskId,
      @NotNull K8sEventCollectionBundle k8sEventCollectionBundle) {
    boolean resetSuccessful = k8sWatchTaskService.resetTask(accountId, taskId, k8sEventCollectionBundle);
    return ResponseDTO.newResponse(resetSuccessful);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/delete")
  @ApiOperation(value = "delete", nickname = "delete perpetual task")
  public ResponseDTO<Boolean> delete(@NotEmpty @QueryParam(K8sWatchTaskResourceClient.ACCOUNT_ID) String accountId,
      @NotEmpty @QueryParam(K8sWatchTaskResourceClient.TASK_ID) String taskId) {
    boolean deletedSuccessfully = k8sWatchTaskService.delete(accountId, taskId);
    return ResponseDTO.newResponse(deletedSuccessfully);
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/status")
  @ApiOperation(value = "status", nickname = "status of a perpetual task")
  public ResponseDTO<PerpetualTaskRecord> status(
      @NotEmpty @QueryParam(K8sWatchTaskResourceClient.TASK_ID) String taskId) {
    final PerpetualTaskRecord perpetualTaskRecord = k8sWatchTaskService.getStatus(taskId);
    return ResponseDTO.newResponse(perpetualTaskRecord);
  }
}
