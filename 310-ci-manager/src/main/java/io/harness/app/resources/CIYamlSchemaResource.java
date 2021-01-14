package io.harness.app.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.app.intfc.CIYamlSchemaService;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.yaml.schema.YamlSchemaResource;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Api("/yaml-schema")
@Path("/yaml-schema")
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
  @ApiOperation(value = "Get Yaml Schema", nickname = "getYamlSchema")
  public ResponseDTO<PartialSchemaDTO> getYamlSchema(
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope) {
    JsonNode schema = ciYamlSchemaService.getIntegrationStageYamlSchema(orgIdentifier, projectIdentifier, scope);
    return ResponseDTO.newResponse(PartialSchemaDTO.builder()
                                       .nodeName(IntegrationStageConfig.class.getSimpleName())
                                       .nodeType(getIntegrationStageTypeName())
                                       .schema(schema)
                                       .build());
  }

  private String getIntegrationStageTypeName() {
    JsonTypeName annotation = IntegrationStageConfig.class.getAnnotation(JsonTypeName.class);
    return annotation.value();
  }
}
