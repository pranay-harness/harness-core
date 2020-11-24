package io.harness.yaml.snippets.resources;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.yaml.dto.YamlSnippetsDTO;
import io.harness.yaml.impl.YamlSnippetProvider;
import io.harness.yaml.snippets.SnippetTag;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Api("yaml-snippet")
@Path("yaml-snippet")
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class YamlSnippetResource {
  YamlSnippetProvider yamlSnippetProvider;

  /**
   * @param tags list of tags which must be present in metadata of snippet.
   * @return list of Metadata which contains all the tags.
   */
  @GET
  @ApiOperation(value = "Get Yaml Snippet Metadata", nickname = "getYamlSnippetMetadata")
  public ResponseDTO<YamlSnippetsDTO> getYamlSnippetsMetaData(@QueryParam("tags") List<SnippetTag> tags) {
    final List<String> tagList = tags.stream().map(Enum::name).collect(Collectors.toList());
    return ResponseDTO.newResponse(yamlSnippetProvider.getYamlSnippetMetaData(tagList));
  }

  /**
   * @param identifier slug of snippet.
   * @return the snippet.
   */
  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get Yaml Snippet", nickname = "getYamlSnippet")
  public ResponseDTO<String> getYamlSnippet(@PathParam("identifier") String identifier) {
    return ResponseDTO.newResponse(yamlSnippetProvider.getYamlSnippet(identifier));
  }
}
