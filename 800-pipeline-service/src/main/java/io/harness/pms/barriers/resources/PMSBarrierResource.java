/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.barriers.resources;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.pms.barriers.mapper.BarrierExecutionInfoDTOMapper;
import io.harness.pms.barriers.mapper.BarrierInfoDTOMapper;
import io.harness.pms.barriers.mapper.BarrierSetupInfoDTOMapper;
import io.harness.pms.barriers.response.BarrierExecutionInfoDTO;
import io.harness.pms.barriers.response.BarrierInfoDTO;
import io.harness.pms.barriers.response.BarrierSetupInfoDTO;
import io.harness.pms.barriers.service.PMSBarrierService;
import io.harness.steps.barriers.beans.BarrierSetupInfo;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("/barriers")
@Path("/barriers")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
public class PMSBarrierResource {
  private final PMSBarrierService pmsBarrierService;

  @PUT
  @Path("/setupInfo")
  @ApiOperation(value = "Gets barriers setup info list", nickname = "getBarriersSetupInfoList")
  public ResponseDTO<List<BarrierSetupInfoDTO>> getBarriersSetupInfoList(
      @NotNull @ApiParam(hidden = true) String yaml) {
    List<BarrierSetupInfo> barrierSetupInfoList = pmsBarrierService.getBarrierSetupInfoList(yaml);
    List<BarrierSetupInfoDTO> barrierSetupInfoDTOList =
        barrierSetupInfoList.stream().map(BarrierSetupInfoDTOMapper.toBarrierSetupInfoDTO).collect(Collectors.toList());
    return ResponseDTO.newResponse(barrierSetupInfoDTOList);
  }

  @GET
  @Path("/executionInfo")
  @ApiOperation(value = "Gets barriers execution info list", nickname = "getBarriersExecutionInfo")
  public ResponseDTO<List<BarrierExecutionInfoDTO>> getBarriersExecutionInfo(
      @NotNull @QueryParam("stageSetupId") String stageSetupId,
      @NotNull @QueryParam("planExecutionId") String planExecutionId) {
    List<BarrierExecutionInfo> barrierExecutionInfoList =
        pmsBarrierService.getBarrierExecutionInfoList(stageSetupId, planExecutionId);
    List<BarrierExecutionInfoDTO> barrierExecutionInfoDTOList =
        barrierExecutionInfoList.stream()
            .map(BarrierExecutionInfoDTOMapper.toBarrierExecutionInfoDTO)
            .collect(Collectors.toList());
    return ResponseDTO.newResponse(barrierExecutionInfoDTOList);
  }

  @GET
  @Path("/info")
  @ApiOperation(value = "Gets barriers info", nickname = "getBarrierInfo")
  public ResponseDTO<BarrierInfoDTO> getBarriersInfo(@NotNull @QueryParam("barrierSetupId") String barrierSetupId,
      @NotNull @QueryParam("planExecutionId") String planExecutionId) {
    BarrierExecutionInfo barrierExecutionInfo =
        pmsBarrierService.getBarrierExecutionInfo(barrierSetupId, planExecutionId);
    return ResponseDTO.newResponse(BarrierInfoDTOMapper.toBarrierInfoDTO.apply(barrierExecutionInfo));
  }
}
