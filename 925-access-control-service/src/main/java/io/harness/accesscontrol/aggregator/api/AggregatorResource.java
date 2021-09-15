/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
public class AggregatorResource {
  private final AggregatorService aggregatorService;

  @Inject
  public AggregatorResource(AggregatorService aggregatorResource) {
    this.aggregatorService = aggregatorResource;
  }

  @POST
  @Path("request-secondary-sync")
  @ApiOperation(value = "Trigger Secondary Sync", nickname = "triggerSecondarySync")
  @InternalApi
  public ResponseDTO<AggregatorSecondarySyncState> triggerSecondarySync() {
    AggregatorSecondarySyncState aggregatorSecondarySyncState = aggregatorService.requestSecondarySync();
    return ResponseDTO.newResponse(aggregatorSecondarySyncState);
  }

  @POST
  @Path("request-switch-to-primary")
  @ApiOperation(value = "Switch To Primary", nickname = "switchToPrimary")
  @InternalApi
  public ResponseDTO<AggregatorSecondarySyncState> switchToPrimary() {
    AggregatorSecondarySyncState aggregatorSecondarySyncState = aggregatorService.requestSwitchToPrimary();
    return ResponseDTO.newResponse(aggregatorSecondarySyncState);
  }
}
