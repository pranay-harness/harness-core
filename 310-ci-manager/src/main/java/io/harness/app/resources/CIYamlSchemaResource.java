package io.harness.app.resources;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.app.intfc.CIYamlSchemaService;
import io.harness.ci.plan.creator.execution.CIPipelineModuleInfo;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.yaml.schema.YamlSchemaResource;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@OwnedBy(CI)
@Api("/partial-yaml-schema")
@Path("/partial-yaml-schema")
@Produces({"application/json", "text/yaml", "text/html", "text/plain"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class CIYamlSchemaResource implements YamlSchemaResource {
  CIYamlSchemaService ciYamlSchemaService;

  @GET
  @ApiOperation(value = "Get Partial Yaml Schema", nickname = "getPartialYamlSchema")
  public ResponseDTO<List<PartialSchemaDTO>> getYamlSchema(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    List<PartialSchemaDTO> schema =
        ciYamlSchemaService.getIntegrationStageYamlSchema(orgIdentifier, projectIdentifier, scope);
    return ResponseDTO.newResponse(schema);
  }

  @GET
  @ApiOperation(value = "dummy api for checking integration stage", nickname = "dummyApiForSwaggerStageSchemaCheck")
  @Path("/dummyApiForSwaggerStageSchemaCheck")
  // DO NOT DELETE THIS WITHOUT CONFIRMING WITH UI
  public ResponseDTO<StageElementConfig> dummyApiForSwaggerSchemaCheckForStage() {
    return ResponseDTO.newResponse(StageElementConfig.builder().build());
  }

  @GET
  @ApiOperation(value = "dummy api for checking integration stage", nickname = "dummyApiForSwaggerStepSchemaCheck")
  @Path("/dummyApiForSwaggerStepSchemaCheck")
  // DO NOT DELETE THIS WITHOUT CONFIRMING WITH UI
  public ResponseDTO<StepElementConfig> dummyApiForSwaggerSchemaCheckForStep() {
    return ResponseDTO.newResponse(StepElementConfig.builder().build());
  }

  @GET
  @ApiOperation(value = "dummy api for checking CIPipelineModuleInfo",
      nickname = "dummyApiForSwaggerCIPipelineModuleInfoSchemaCheck")
  @Path("/dummyApiForSwaggerCIPipelineModuleInfoSchemaCheck")
  // DO NOT DELETE THIS WITHOUT CONFIRMING WITH UI
  public ResponseDTO<CIPipelineModuleInfo>
  dummyApiForSwaggerSchemaCheckForCIPipelineModuleInfo() {
    return ResponseDTO.newResponse(CIPipelineModuleInfo.builder().build());
  }

  @GET
  @Path("/details")
  @ApiOperation(value = "Get Partial Yaml Schema with details", nickname = "getPartialYamlSchemaWithDetails")
  public ResponseDTO<YamlSchemaDetailsWrapper> getYamlSchemaWithDetails(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    List<YamlSchemaWithDetails> schemaWithDetails =
        ciYamlSchemaService.getIntegrationStageYamlSchemaWithDetails(orgIdentifier, projectIdentifier, scope);
    return ResponseDTO.newResponse(
        YamlSchemaDetailsWrapper.builder().yamlSchemaWithDetailsList(schemaWithDetails).build());
  }

  @POST
  @Path("/merged")
  @ApiOperation(value = "Get Merged Partial Yaml Schema", nickname = "getMergedPartialYamlSchema")
  public ResponseDTO<PartialSchemaDTO> getMergedYamlSchema(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope,
      @RequestBody(required = true,
          description = "Step Schema with details") YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper) {
    PartialSchemaDTO schema = ciYamlSchemaService.getMergedIntegrationStageYamlSchema(
        projectIdentifier, orgIdentifier, scope, yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList());
    return ResponseDTO.newResponse(schema);
  }

  @GET
  @Path("/step")
  @ApiOperation(value = "Get step YAML schema", nickname = "getStepYamlSchema")
  public ResponseDTO<JsonNode> getStepYamlSchema(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ENTITY_TYPE) EntityType entityType) {
    return ResponseDTO.newResponse(ciYamlSchemaService.getStepYamlSchema(entityType));
  }
}
