/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.ConnectorConstants.CONNECTOR_TYPES;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.yaml.schema.beans.SchemaConstants.CONST_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ENUM_NODE;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
@NextGenManagerAuth
public class NgCoreYamlSchemaResource implements YamlSchemaResource {
  YamlSchemaProvider yamlSchemaProvider;

  @GET
  @ApiOperation(value = "Get Yaml Schema", nickname = "getYamlSchema")
  public ResponseDTO<JsonNode> getYamlSchema(@QueryParam("entityType") @NotNull EntityType entityType,
      @QueryParam("subtype") ConnectorType entitySubtype,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @QueryParam("scope") Scope scope,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    JsonNode schema = yamlSchemaProvider.getYamlSchema(entityType, orgIdentifier, projectIdentifier, scope);
    if (schema == null) {
      throw new NotFoundException(String.format("No schema found for entity type %s ", entityType.getYamlName()));
    }
    if (entityType == EntityType.CONNECTORS && entitySubtype != null) {
      schema = yamlSchemaProvider.updateArrayFieldAtSecondLevelInSchema(
          schema, CONNECTOR_TYPES, ENUM_NODE, entitySubtype.getDisplayName());
    }
    if (isNotEmpty(identifier)) {
      schema = yamlSchemaProvider.upsertInObjectFieldAtSecondLevelInSchema(
          schema, NGCommonEntityConstants.IDENTIFIER_KEY, CONST_NODE, identifier);
    }
    return ResponseDTO.newResponse(schema);
  }
}
