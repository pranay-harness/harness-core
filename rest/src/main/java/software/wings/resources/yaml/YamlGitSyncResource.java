package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.YamlGitSync;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by bsollish
 */
@Api("git-sync/yaml")
@Path("git-sync/yaml")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@AuthRule(ResourceType.SETTING)
public class YamlGitSyncResource {
  private YamlGitSyncService yamlGitSyncService;

  /**
   * Instantiates a new service resource.
   *
   * @param yamlGitSyncService the yaml git sync service
   */
  @Inject
  public YamlGitSyncResource(YamlGitSyncService yamlGitSyncService) {
    this.yamlGitSyncService = yamlGitSyncService;
  }

  /**
   * Gets the yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("/{type}/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> getYamlGitSync(@PathParam("type") String type,
      @PathParam("entityId") String entityId, @QueryParam("accountId") String accountId) {
    return yamlGitSyncService.getYamlGitSync(type, entityId, accountId);
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   * @param accountId the account id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @POST
  @Path("/{type}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> saveYamlGitSync(
      @PathParam("type") String type, @QueryParam("accountId") String accountId, YamlGitSync yamlGitSync) {
    return yamlGitSyncService.saveYamlGitSync(type, accountId, yamlGitSync);
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   *@param entityId the uuid of the entity
   * @param accountId the account id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @PUT
  @Path("/{type}/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> updateYamlGitSync(@PathParam("type") String type,
      @PathParam("entityId") String entityId, @QueryParam("accountId") String accountId, YamlGitSync yamlGitSync) {
    return yamlGitSyncService.updateYamlGitSync(type, entityId, accountId, yamlGitSync);
  }
}
