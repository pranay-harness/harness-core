package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
public class YamlSchemaResource {
  YamlSchemaHelper yamlSchemaHelper;
  YamlSchemaSubtypeHelper yamlSchemaSubtypeHelper;

  @GET
  @ApiOperation(value = "Get Yaml Schema", nickname = "getYamlSchema")
  public ResponseDTO<String> getYamlSchema(@QueryParam("entityType") @NotNull EntityType entityType) {
    final String schemaForEntityType = yamlSchemaHelper.getSchemaForEntityType(entityType);
    if (isEmpty(schemaForEntityType)) {
      throw new InvalidRequestException(String.format("No schema found for entity type %s ", entityType.getYamlName()));
    }
    return ResponseDTO.newResponse(schemaForEntityType);
  }

  // todo(abhinav): Currently handled only for connector subtype, handle it generically for all subtypes.
  @GET
  @Path("{entityType}")
  @ApiOperation(value = "Get Yaml Schema for subtype", nickname = "getYamlSchemaForSubtype")
  public ResponseDTO<String> getYamlSchemaForSubtype(@PathParam("entityType") @NotNull EntityType entityType,
      @QueryParam("subtype") @NotNull ConnectorType entitySubtype) {
    final String schemaForEntityType = yamlSchemaHelper.getSchemaForEntityType(entityType);
    if (isEmpty(schemaForEntityType)) {
      throw new InvalidRequestException(String.format("No schema found for entity type %s ", entityType.getYamlName()));
    }
    final String schemaForSubtype =
        yamlSchemaSubtypeHelper.getSchemaForSubtype(entityType, entitySubtype, schemaForEntityType);
    return ResponseDTO.newResponse(schemaForSubtype);
  }
}
