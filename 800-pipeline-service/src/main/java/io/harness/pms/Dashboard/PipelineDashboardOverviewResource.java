package io.harness.pms.Dashboard;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.pipeline.service.PipelineDashboardService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Api("pipelines")
@Path("pipelines")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@PipelineServiceAuth
@Slf4j
public class PipelineDashboardOverviewResource {
  private final PipelineDashboardService pipelineDashboardService;
  @GET
  @Path("/pipelineHealth")
  @ApiOperation(value = "Get pipeline health", nickname = "getPipelinedHealth")
  public ResponseDTO<DashboardPipelineHealthInfo> getPipelinedHealth(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @NotNull @QueryParam("moduleInfo") String moduleInfo,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    long interval = ChronoUnit.DAYS.between(startDate, endDate);

    if (interval < 0) {
      interval = interval * (-1);
    }

    LocalDate previousStartDate = startDate.minusDays(interval);
    return ResponseDTO.newResponse(
        pipelineDashboardService.getDashboardPipelineHealthInfo(accountIdentifier, orgIdentifier, projectIdentifier,
            pipelineIdentifier, startInterval, endInterval, previousStartDate.toString(), moduleInfo));
  }

  @GET
  @Path("/pipelineExecution")
  @ApiOperation(value = "Get pipeline execution", nickname = "getPipelineExecution")
  public ResponseDTO<DashboardPipelineExecutionInfo> getPipelineExecution(
      @NotNull @QueryParam("accountId") String accountIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @NotNull @QueryParam("moduleInfo") String moduleInfo,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "startInterval") String startInterval,
      @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date should be in yyyy-mm-dd format") @QueryParam(
          "endInterval") String endInterval) {
    return ResponseDTO.newResponse(pipelineDashboardService.getDashboardPipelineExecutionInfo(accountIdentifier,
        orgIdentifier, projectIdentifier, pipelineIdentifier, startInterval, endInterval, moduleInfo));
  }
}
