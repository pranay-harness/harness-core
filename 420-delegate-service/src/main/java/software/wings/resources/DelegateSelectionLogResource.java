/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateSelectionLogsService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("selection-logs")
@Path("/selection-logs")
@Produces("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(DEL)
@Tag(name = "Delegate Selection Logs", description = "Contains APIs related to Delegate Selection logs")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })

public class DelegateSelectionLogResource {
  private DelegateSelectionLogsService delegateSelectionLogsService;

  @Inject
  public DelegateSelectionLogResource(DelegateSelectionLogsService delegateSelectionLogsService) {
    this.delegateSelectionLogsService = delegateSelectionLogsService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @Operation(operationId = "getSelectionLogs", summary = "Retrieves list of Selection Logs for the Task Id.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "A list of Task Selection logs. It includes delegateId, delegateType, delegateName, "
                + "delegateHostName, delegateProfileName, conclusion, message, eventTimestamp and profileScopingRulesDetails")
      })
  public RestResponse<List<DelegateSelectionLogParams>>
  getSelectionLogs(@Parameter(description = "Account id") @QueryParam("accountId") String accountId,
      @Parameter(description = "Task id") @QueryParam("taskId") String taskId) {
    return new RestResponse(delegateSelectionLogsService.fetchTaskSelectionLogs(accountId, taskId));
  }

  @GET
  @Path("/v2")
  @Timed
  @ExceptionMetered
  @Operation(operationId = "getTaskSelectionLogsData",
      summary = "Retrieves list of Selection Logs and map of preview setup for selection logs for the given task id.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "A list of Task Selection Logs and a map of preview setup abstractions. It includes delegateId, delegateType, delegateName, "
                + "delegateHostName, delegateProfileName, conclusion, message, eventTimestamp and profileScopingRulesDetails")
      })
  public RestResponse<DelegateSelectionLogResponse>
  getSelectionLogsV2(@Parameter(description = "Account id") @QueryParam("accountId") String accountId,
      @Parameter(description = "Task id") @QueryParam("taskId") String taskId) {
    return new RestResponse(delegateSelectionLogsService.fetchTaskSelectionLogsData(accountId, taskId));
  }
}
