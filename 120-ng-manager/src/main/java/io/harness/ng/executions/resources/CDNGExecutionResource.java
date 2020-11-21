package io.harness.ng.executions.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@Api("executions")
@Path("executions")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class CDNGExecutionResource {
  private final NgPipelineExecutionServiceImpl executionService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  public ResponseDTO<PageResponse<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId,
      @QueryParam("serviceIdentifiers") List<String> serviceIdentifiers,
      @QueryParam("envIdentifiers") List<String> environmentIdentifiers,
      @QueryParam("pipelineIdentifiers") List<String> pipelineIdentifiers,
      @QueryParam("executionStatuses") List<ExecutionStatus> executionStatuses, @QueryParam("startTime") Long startTime,
      @QueryParam("endTime") Long endTime, @QueryParam("searchTerm") String searchTerm,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort) {
    log.info("Get List of executions");
    if (sort.isEmpty()) {
      sort = Collections.singletonList(PipelineExecutionSummaryKeys.startedAt + ",desc");
    }
    PipelineExecutionSummaryFilter pipelineExecutionSummaryFilter = PipelineExecutionSummaryFilter.builder()
                                                                        .searchTerm(searchTerm)
                                                                        .serviceIdentifiers(serviceIdentifiers)
                                                                        .envIdentifiers(environmentIdentifiers)
                                                                        .pipelineIdentifiers(pipelineIdentifiers)
                                                                        .startTime(startTime)
                                                                        .endTime(endTime)
                                                                        .executionStatuses(executionStatuses)
                                                                        .build();
    Page<PipelineExecutionSummaryDTO> pipelines =
        executionService
            .getExecutions(
                accountId, orgId, projectId, getPageRequest(page, size, sort), pipelineExecutionSummaryFilter)
            .map(ExecutionToDtoMapper::writeExecutionDto);
    return ResponseDTO.newResponse(getNGPageResponse(pipelines));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Execution Detail", nickname = "getPipelineExecutionDetail")
  @Path("/{planExecutionId}")
  public ResponseDTO<PipelineExecutionDetail> getPipelineExecutionDetail(
      @NotNull @QueryParam("accountIdentifier") String accountId, @NotNull @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull @PathParam("planExecutionId") String planExecutionId,
      @QueryParam("stageIdentifier") String stageIdentifier) {
    return ResponseDTO.newResponse(executionService.getPipelineExecutionDetail(planExecutionId, stageIdentifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Execution Status list", nickname = "getExecutionStatuses")
  @Path("/executionStatus")
  public ResponseDTO<List<ExecutionStatus>> getExecutionStatuses() {
    return ResponseDTO.newResponse(executionService.getExecutionStatuses());
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Step Types to Yaml Type Mapping", nickname = "getStepTypesToYamlTypeMapping")
  @Path("/step-types")
  public ResponseDTO<Map<ExecutionNodeType, String>> getStepTypeToYamlTypeMapping(
      @QueryParam("executionNodeType") ExecutionNodeType executionNodeType) {
    return ResponseDTO.newResponse(executionService.getStepTypeToYamlTypeMapping());
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "pause, resume or stop the pipeline executions", nickname = "handleInterrupt")
  @Path("/interrupt/{planExecutionId}")
  public ResponseDTO<PipelineExecutionInterruptDTO> handleInterrupt(
      @NotNull @QueryParam("accountIdentifier") String accountId, @NotNull @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId,
      @NotNull @QueryParam("interruptType") PipelineExecutionInterruptType executionInterruptType,
      @NotNull @PathParam("planExecutionId") String planExecutionId) {
    return ResponseDTO.newResponse(executionService.registerInterrupt(executionInterruptType, planExecutionId));
  }
}
