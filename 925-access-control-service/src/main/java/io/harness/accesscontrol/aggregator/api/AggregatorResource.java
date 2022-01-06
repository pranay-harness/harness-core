/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.aggregator.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.aggregator.AggregatorService;
import io.harness.aggregator.models.AggregatorSecondarySyncState;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@OwnedBy(PL)
@Api("/aggregator")
@Path("/aggregator")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "aggregator", description = "This contains the APIs to change the state of the Aggregator")
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
public class AggregatorResource {
  private final AggregatorService aggregatorService;

  @Inject
  public AggregatorResource(AggregatorService aggregatorResource) {
    this.aggregatorService = aggregatorResource;
  }

  @POST
  @Path("request-secondary-sync")
  @ApiOperation(value = "Trigger Secondary Sync", nickname = "triggerSecondarySync", hidden = true)
  @Operation(operationId = "triggerSecondarySync", summary = "Trigger Secondary Sync for Access Control List (ACL)",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Updated status of the Aggregator Secondary Sync State")
      },
      hidden = true)
  @InternalApi
  public ResponseDTO<AggregatorSecondarySyncState>
  triggerSecondarySync() {
    AggregatorSecondarySyncState aggregatorSecondarySyncState = aggregatorService.requestSecondarySync();
    return ResponseDTO.newResponse(aggregatorSecondarySyncState);
  }

  @POST
  @Path("request-switch-to-primary")
  @ApiOperation(value = "Switch To Primary", nickname = "switchToPrimary", hidden = true)
  @Operation(operationId = "requestSwitchToPrimary",
      summary = "Request Access Control Service to make the secondary ACL as the primary ACL",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Updated status of the Aggregator Secondary Sync State")
      },
      hidden = true)
  @InternalApi
  public ResponseDTO<AggregatorSecondarySyncState>
  switchToPrimary() {
    AggregatorSecondarySyncState aggregatorSecondarySyncState = aggregatorService.requestSwitchToPrimary();
    return ResponseDTO.newResponse(aggregatorSecondarySyncState);
  }
}
