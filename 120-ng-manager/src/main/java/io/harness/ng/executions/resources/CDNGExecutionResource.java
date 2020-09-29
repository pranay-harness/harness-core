package io.harness.ng.executions.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionDetail;
import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.RestQueryFilterParser;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
  private final RestQueryFilterParser restQueryFilterParser;
  private final NgPipelineExecutionServiceImpl executionService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Executions list", nickname = "getListOfExecutions")
  public ResponseDTO<PageResponse<PipelineExecutionSummaryDTO>> getListOfExecutions(
      @NotNull @QueryParam("accountIdentifier") String accountId, @QueryParam("orgIdentifier") String orgId,
      @NotNull @QueryParam("projectIdentifier") String projectId, @QueryParam("filter") String filterQuery,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("10") int size,
      @QueryParam("sort") List<String> sort) {
    logger.info("Get List of executions");
    Page<PipelineExecutionSummaryDTO> pipelines =
        executionService.getExecutions(accountId, orgId, projectId, getPageRequest(page, size, sort))
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
}
