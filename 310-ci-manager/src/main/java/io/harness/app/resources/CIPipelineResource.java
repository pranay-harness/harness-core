package io.harness.app.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
  public RestResponse<String> runPipeline(@NotNull @QueryParam("accountIdentifier") String accountId,
      @QueryParam("orgIdentifier") String orgId, @QueryParam("projectIdentifier") String projectId,
      @PathParam("identifier") @NotEmpty String pipelineId, @QueryParam("branch") String branch,
      @QueryParam("tag") String tag) {
    if ((isEmpty(branch) && isEmpty(tag)) || (!isEmpty(branch) && !isEmpty(tag))) {
      throw new InvalidArgumentsException("Either one of branch or tag needs to be set");
    }

    try {
      NgPipelineEntity ngPipelineEntity = ngPipelineService.getPipeline(pipelineId, accountId, orgId, projectId);
      BuildNumberDetails buildNumberDetails = buildNumberService.increaseBuildNumber(accountId, orgId, projectId);

      CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder().buildNumberDetails(buildNumberDetails).build();
      if (!isEmpty(branch)) {
        ciExecutionArgs.setExecutionSource(ManualExecutionSource.builder().branch(branch).build());
      } else {
        ciExecutionArgs.setExecutionSource(ManualExecutionSource.builder().tag(tag).build());
      }
      ciPipelineExecutionService.executePipeline(
          ngPipelineEntity, ciExecutionArgs, buildNumberDetails.getBuildNumber());
    } catch (Exception e) {
      log.error("Failed to run input pipeline ", e);
      throw e;
    }

    return null;
  }
}
