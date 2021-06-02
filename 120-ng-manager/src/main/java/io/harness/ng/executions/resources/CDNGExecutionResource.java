package io.harness.ng.executions.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.executions.beans.CDPipelineModuleInfo;
import io.harness.cdng.pipeline.executions.beans.CDStageModuleInfo;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionInterruptType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummaryFilter;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionInterruptDTO;
import io.harness.ngpipeline.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.execution.ExecutionStatus;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

@OwnedBy(CDC)
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
  @ApiOperation(value = "Gets Execution Status list", nickname = "getExecutionStatuses")
  @Path("/executionStatus")
  public ResponseDTO<List<ExecutionStatus>> getExecutionStatuses() {
    return ResponseDTO.newResponse(executionService.getExecutionStatuses());
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

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "dummy api", nickname = "getDummyCDStageModuleInfo")
  @Path("/dummyCDStageModuleInfo")
  public ResponseDTO<CDStageModuleInfo> getDummyCDStageModuleInfo() {
    return ResponseDTO.newResponse(CDStageModuleInfo.builder().nodeExecutionId("node1").build());
  }

  @GET
  @ApiOperation(value = "dummy api for checking pms schema", nickname = "dummyApiForSwaggerSchemaCheck")
  @Path("/dummyApiForSwaggerSchemaCheck")
  // DO NOT DELETE THIS WITHOUT CONFIRMING WITH UI
  public ResponseDTO<PipelineConfig> dummyApiForSwaggerSchemaCheck() {
    log.info("Get pipeline");
    return ResponseDTO.newResponse(PipelineConfig.builder().build());
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "dummy api", nickname = "getDummyCDPipelineModuleInfo")
  @Path("/dummyCDPipelineModuleInfo")
  public ResponseDTO<CDPipelineModuleInfo> getDummyCDPipelineModuleInfo() {
    List<String> serviceIdentifiers = new ArrayList<>();
    serviceIdentifiers.add("dummyService1");
    List<String> envIdentifiers = new ArrayList<>();
    envIdentifiers.add("DymmyEnv1");
    List<String> serviceDefinitionTypes = new ArrayList<>();
    serviceDefinitionTypes.add("DummyTYpe1");
    List<EnvironmentType> environmentTypes = new ArrayList<>();
    environmentTypes.add(EnvironmentType.PreProduction);
    return ResponseDTO.newResponse(CDPipelineModuleInfo.builder()
                                       .serviceIdentifiers(serviceIdentifiers)
                                       .envIdentifiers(envIdentifiers)
                                       .serviceDefinitionTypes(serviceDefinitionTypes)
                                       .environmentTypes(environmentTypes)
                                       .build());
  }
}
