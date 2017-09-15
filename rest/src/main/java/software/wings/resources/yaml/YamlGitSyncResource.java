package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.Api;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by bsollish
 */
@Api("git-sync/yaml")
@Path("git-sync/yaml")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@AuthRule(ResourceType.SETTING)
public class YamlGitSyncResource {
  /**
   * Gets the yaml git sync info by object type and entitytId (uuid)
   *
   * @param accountId the account id
   * @return the rest response
   */

  /*
  @GET
  @Path("/directory")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> getYamlGitSync(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(yamlDirectoryService.getDirectory(accountId));
  }
  */
}
