/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.snippets.resources;

import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.yaml.snippets.SnippetTag;
import io.harness.yaml.snippets.YamlSnippetResource;
import io.harness.yaml.snippets.dto.YamlSnippetsDTO;
import io.harness.yaml.snippets.impl.YamlSnippetProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Api("yaml-snippet")
@Path("yaml-snippet")
@Produces({"application/json", "text/yaml", "text/html", "text/plain"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class NgCoreYamlSnippetResource implements YamlSnippetResource {
  YamlSnippetProvider yamlSnippetProvider;

  /**
   * @param tags list of tags which must be present in metadata of snippet.
   * @return list of Metadata which contains all the tags.
   */
  @GET
  @ApiOperation(value = "Get Yaml Snippet Metadata", nickname = "getYamlSnippetMetadata")
  public ResponseDTO<YamlSnippetsDTO> getYamlSnippetsMetaData(@QueryParam("tags") @NotNull List<SnippetTag> tags) {
    final List<String> tagList = tags.stream().map(Enum::name).collect(Collectors.toList());
    final YamlSnippetsDTO yamlSnippetMetaData = yamlSnippetProvider.getYamlSnippetMetaData(tagList);
    if (yamlSnippetMetaData == null) {
      throw new NotFoundException("No Snippet Metadata found for given tags");
    }
    return ResponseDTO.newResponse(yamlSnippetMetaData);
  }

  /**
   * @param identifier slug of snippet.
   * @return the snippet.
   */
  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get Yaml Snippet", nickname = "getYamlSnippet")
  public ResponseDTO<JsonNode> getYamlSnippet(@PathParam("identifier") @NotNull String identifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam("scope") Scope scope) {
    final JsonNode yamlSnippet =
        yamlSnippetProvider.getYamlSnippet(identifier, orgIdentifier, projectIdentifier, scope);
    if (yamlSnippet == null) {
      throw new NotFoundException("No Snippet found for given identifier");
    }
    return ResponseDTO.newResponse(yamlSnippet);
  }
}
