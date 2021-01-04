package io.harness.app.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.exception.ngexception.CIStageExecutionUserException;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("ci")
@Path("/ci")
@NextGenManagerAuth
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class CIPipelineResource {
  private NGPipelineService ngPipelineService;
  private CIPipelineExecutionService ciPipelineExecutionService;
  private BuildNumberService buildNumberService;

  @POST
  @Path("/execute/{identifier}/run")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Execute a CI pipeline", nickname = "executePipeline")
  public ResponseDTO runPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("identifier") @NotEmpty String pipelineId, @QueryParam("branch") String branch,
      @QueryParam("tag") String tag) {
    if ((isEmpty(branch) && isEmpty(tag)) || (!isEmpty(branch) && !isEmpty(tag))) {
      return ResponseDTO.newResponse(FailureDTO.toBody(Status.ERROR, ErrorCode.INVALID_REQUEST,
          String.format(
              "ERROR: Either one of branch or tag needs to be set to execute pipeline with identifier: %s in projectIdentifier  %s, orgIdentifier  %s, accountIdentifier %s",
              pipelineId, projectId, orgId, accountId),
          null));
    }
    NgPipelineEntity ngPipelineEntity;
    try {
      ngPipelineEntity = ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId);
    } catch (Exception e) {
      return ResponseDTO.newResponse(FailureDTO.toBody(Status.ERROR, ErrorCode.INVALID_REQUEST,
          String.format(
              "ERROR:  Failed to get pipeline with identifier: %s in projectIdentifier %s, orgIdentifier  %s, accountIdentifier %s",
              pipelineId, projectId, orgId, accountId),
          null));
    }
    BuildNumberDetails buildNumberDetails = buildNumberService.increaseBuildNumber(accountId, orgId, projectId);

    try {
      CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder().buildNumberDetails(buildNumberDetails).build();
      if (!isEmpty(branch)) {
        ciExecutionArgs.setExecutionSource(ManualExecutionSource.builder().branch(branch).build());
      } else {
        ciExecutionArgs.setExecutionSource(ManualExecutionSource.builder().tag(tag).build());
      }
      ciPipelineExecutionService.executePipeline(
          ngPipelineEntity, ciExecutionArgs, buildNumberDetails.getBuildNumber());
    } catch (CIStageExecutionException ex) {
      return ResponseDTO.newResponse(ErrorDTO.newError(Status.ERROR, ErrorCode.NG_PIPELINE_EXECUTION_EXCEPTION,
          String.format(
              "Failed to execute pipeline identifier: %s with build number: %s accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s. ERROR: "
                  + ex.getMessage(),
              pipelineId, buildNumberDetails.getBuildNumber().toString(), accountId, orgId, projectId),
          null));

    } catch (CIStageExecutionUserException ex) {
      return ResponseDTO.newResponse(FailureDTO.toBody(Status.ERROR, ErrorCode.INVALID_REQUEST,
          String.format(
              "Failed to execute pipeline identifier: %s with build number: %s accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s. ERROR:"
                  + ex.getMessage(),
              pipelineId, buildNumberDetails.getBuildNumber().toString(), accountId, orgId, projectId),
          null));

    } catch (Exception ex) {
      return ResponseDTO.newResponse(ErrorDTO.newError(Status.ERROR, ErrorCode.UNKNOWN_ERROR,
          String.format(
              "Failed to execute pipeline for identifier with build number: %s in accountIdentifier: %s, orgIdentifier: %s, projectIdentifier: %s ERROR: %s",
              pipelineId, buildNumberDetails.getBuildNumber().toString(), projectId, orgId, accountId, ex.getMessage()),
          null));
    }

    return ResponseDTO.newResponse(
        CIStageExecutionResponse.builder()
            .message(String.format(
                "Successfully submitted the execution for pipeline identifier: %s with build number: %s in projectIdentifier: %s, orgIdentifier: %s, accountIdentifier: %s",
                pipelineId, buildNumberDetails.getBuildNumber().toString(), projectId, orgId, accountId))
            .status(Status.SUCCESS)
            .build());
  }
}
