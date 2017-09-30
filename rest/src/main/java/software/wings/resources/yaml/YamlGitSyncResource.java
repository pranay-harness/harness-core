package software.wings.resources.yaml;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.Type;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
   * Gets the yaml git sync info by uuid
   *
   * @param uuid the uuid
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("/{uuid}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> get(
      @PathParam("uuid") String uuid, @QueryParam("accountId") String accountId, @QueryParam("appId") String appId) {
    return new RestResponse<>(yamlGitSyncService.get(uuid, accountId, appId));
  }

  /**
   * Gets the yaml git sync info by object type and entityId (uuid)
   *
   * @param restName the restName of the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("/{type}/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> get(@PathParam("type") String restName, @PathParam("entityId") String entityId,
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId) {
    Type type = YamlGitSync.convertRestNameToType(restName);
    return new RestResponse<>(yamlGitSyncService.get(type, entityId, accountId, appId));
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param restName the restName of the object type
   * @param accountId the account id
   * @param appId the app id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @POST
  @Path("/{type}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> save(@PathParam("type") String restName, @QueryParam("accountId") String accountId,
      @QueryParam("appId") String appId, YamlGitSync yamlGitSync) {
    Type type = YamlGitSync.convertRestNameToType(restName);
    yamlGitSync.setType(type);
    yamlGitSync.setAccountId(accountId);

    if (appId == null || appId.isEmpty()) {
      if (type == Type.APP) {
        appId = yamlGitSync.getEntityId();
      } else {
        appId = GLOBAL_APP_ID;
      }
    }

    yamlGitSync.setAppId(appId);

    return new RestResponse<>(yamlGitSyncService.save(accountId, appId, yamlGitSync));
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param restName the restName of the object type
   *@param entityId the uuid of the entity
   * @param accountId the account id
   * @param appId the app id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  @PUT
  @Path("/{type}/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> update(@PathParam("type") String restName, @PathParam("entityId") String entityId,
      @QueryParam("accountId") String accountId, @QueryParam("appId") String appId, YamlGitSync yamlGitSync) {
    yamlGitSync.setAccountId(accountId);

    if (appId == null || appId.isEmpty()) {
      appId = GLOBAL_APP_ID;
    }

    yamlGitSync.setAppId(appId);
    Type type = YamlGitSync.convertRestNameToType(restName);
    yamlGitSync.setType(type);

    if (type == Type.FOLDER) {
      try {
        entityId = URLDecoder.decode(entityId, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }

    return new RestResponse<>(yamlGitSyncService.update(entityId, accountId, appId, yamlGitSync));
  }

  /**
   * Catch call from GitHub repo webhook
   *
   * @param accountId the account id
   *
   * @return the rest response
   */
  @POST
  @Path("/webhook")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlGitSync> webhookCatcher(@QueryParam("accountId") String accountId, String rawJson) {
    System.out.println(rawJson);

    return null;
  }
}
