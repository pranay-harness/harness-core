package software.wings.resources;

import static software.wings.beans.RestResponse.Builder.aRestResponse;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.AccountPlugin;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.PluginService;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
@Api("plugins")
@Path("/plugins")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
public class PluginResource {
  private PluginService pluginService;

  @Inject
  public PluginResource(PluginService pluginService) {
    this.pluginService = pluginService;
  }

  @GET
  @Path("{accountId}/installed")
  @Timed
  @ExceptionMetered
  public RestResponse<List<AccountPlugin>> installedPlugins(@PathParam("accountId") String accountId) {
    return aRestResponse().withResource(pluginService.getInstalledPlugins(accountId)).build();
  }

  @GET
  @Path("{accountId}/installed/settingschema")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Map<String, Object>>> installedPluginSettingSchema(
      @PathParam("accountId") String accountId) {
    return aRestResponse().withResource(pluginService.getPluginSettingSchema(accountId)).build();
  }
}
