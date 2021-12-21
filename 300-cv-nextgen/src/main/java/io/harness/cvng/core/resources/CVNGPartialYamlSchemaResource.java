package io.harness.cvng.core.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.cvng.core.services.api.CVNGYamlSchemaService;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.PublicApi;
import io.harness.yaml.schema.YamlSchemaResource;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

import com.google.inject.Inject;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.AllArgsConstructor;

import java.util.List;

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
public class CVNGPartialYamlSchemaResource implements YamlSchemaResource {
  private CVNGYamlSchemaService cvngYamlSchemaService;

  @GET
  @PublicApi // TODO: fix invalid credentials issue.
  @ApiOperation(value = "Get Partial Yaml Schema", nickname = "getPartialYamlSchema")
  public ResponseDTO<PartialSchemaDTO> getYamlSchema(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    PartialSchemaDTO schema =
        cvngYamlSchemaService.getDeploymentStageYamlSchema(orgIdentifier, projectIdentifier, scope);
    return ResponseDTO.newResponse(schema);
  }

  @GET
  @PublicApi // TODO: fix invalid credentials issue.
  @Path("/details")
  @ApiOperation(value = "Get Partial Yaml Schema with details", nickname = "getPartialYamlSchemaWithDetails")
  public ResponseDTO<YamlSchemaDetailsWrapper> getYamlSchemaWithDetails(
          @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
          @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
          @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    List<YamlSchemaWithDetails> schemaWithDetails =
            cvngYamlSchemaService.getDeploymentStageYamlSchemaWithDetails(orgIdentifier, projectIdentifier, scope);
    return ResponseDTO.newResponse(
            YamlSchemaDetailsWrapper.builder().yamlSchemaWithDetailsList(schemaWithDetails).build());
  }

  @POST
  @PublicApi // TODO: fix invalid credentials issue.
  @Path("/merged")
  @ApiOperation(value = "Get Merged Partial Yaml Schema", nickname = "getMergedPartialYamlSchema")
  public ResponseDTO<PartialSchemaDTO> getMergedYamlSchema(
          @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
          @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
          @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope,
          @RequestBody(required = true,
                  description = "Step Schema with details") YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper) {
    PartialSchemaDTO schema = cvngYamlSchemaService.getMergedDeploymentStageYamlSchema(
            projectIdentifier, orgIdentifier, scope, yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList());
    return ResponseDTO.newResponse(schema);
  }

}
