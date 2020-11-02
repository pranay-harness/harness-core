package io.harness.cdng.pipeline.resources;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStrategyType;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.pipeline.helpers.CDNGPipelineConfigurationHelper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("pipelines")
@Path("pipelines/configuration")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class CDNGPipelineConfigurationResource {
  private final CDNGPipelineConfigurationHelper cdngPipelineConfigurationHelper;

  @GET
  @Path("/strategies")
  @ApiOperation(value = "Gets Execution Strategy list", nickname = "getExecutionStrategyList")
  public ResponseDTO<Map<ServiceDefinitionType, List<ExecutionStrategyType>>> getExecutionStrategyList() {
    log.info("Get List of execution Strategy");
    return ResponseDTO.newResponse(cdngPipelineConfigurationHelper.getExecutionStrategyList());
  }

  @GET
  @Path("/strategies/yaml-snippets")
  @ApiOperation(value = "Gets Yaml for Execution Strategy based on deployment type and selected strategy",
      nickname = "getExecutionStrategyYaml")
  public ResponseDTO<String>
  getExecutionStrategyYaml(@NotNull @QueryParam("serviceDefinitionType") ServiceDefinitionType serviceDefinitionType,
      @NotNull @QueryParam("strategyType") ExecutionStrategyType executionStrategyType) throws IOException {
    return ResponseDTO.newResponse(
        cdngPipelineConfigurationHelper.getExecutionStrategyYaml(serviceDefinitionType, executionStrategyType));
  }

  @GET
  @Path("/serviceDefinitionTypes")
  @ApiOperation(value = "Git list of service definition types", nickname = "getServiceDefinitionTypes")
  public ResponseDTO<List<ServiceDefinitionType>> getServiceDefinitionTypes() {
    return ResponseDTO.newResponse(cdngPipelineConfigurationHelper.getServiceDefinitionTypes());
  }

  @GET
  @Path("/steps")
  @ApiOperation(value = "get steps for given service definition type", nickname = "getSteps")
  public ResponseDTO<StepCategory> getSteps(
      @NotNull @QueryParam("serviceDefinitionType") ServiceDefinitionType serviceDefinitionType) {
    return ResponseDTO.newResponse(cdngPipelineConfigurationHelper.getSteps(serviceDefinitionType));
  }
}
