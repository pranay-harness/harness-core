package io.harness.yaml.resources;

import io.harness.yaml.dto.YamlSnippetsDTO;
import io.harness.yaml.impl.YamlSnippetProvider;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@Api("yaml-snippet")
@Path("yaml-snippet")
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class YamlSnippetResource {
  YamlSnippetProvider yamlSnippetProvider;

  /**
   * @param tags list of tags which must be present in metadata of snippet.
   * @return list of Metadata which contains all the tags.
   */
  @GET
  @ApiOperation(value = "Get Yaml Snippet Metadata", nickname = "getYamlSnippetMetadata")
  public YamlSnippetsDTO getYamlSnippetsMetaData(@QueryParam("tags") List<String> tags) {
    return yamlSnippetProvider.getYamlSnippetMetaData(tags);
  }

  /**
   * @param identifier slug of snippet.
   * @return the snippet.
   */
  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get Yaml Snippet", nickname = "getYamlSnippet")
  public String getYamlSnippet(@PathParam("identifier") String identifier) {
    return yamlSnippetProvider.getYamlSnippet(identifier);
  }
}
