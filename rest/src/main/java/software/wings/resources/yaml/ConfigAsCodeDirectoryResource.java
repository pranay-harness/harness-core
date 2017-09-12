package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.directory.DirectoryNode;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Configuration as Code Resource class.
 *
 * @author bsollish
 */
@Api("/configAsCode")
@Path("/configAsCode")
@Produces("application/json")
@AuthRule(SETTING)
public class ConfigAsCodeDirectoryResource {
  private YamlDirectoryService yamlDirectoryService;

  /**
   * Instantiates a new app yaml resource.
   *
   * @param yamlDirectoryService        the yaml directory service
   */
  @Inject
  public ConfigAsCodeDirectoryResource(YamlDirectoryService yamlDirectoryService) {
    this.yamlDirectoryService = yamlDirectoryService;
  }

  /**
   * Gets the config as code directory by accountId
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DirectoryNode> get(@PathParam("accountId") String accountId) {
    return new RestResponse<>(yamlDirectoryService.get(accountId));
  }
}