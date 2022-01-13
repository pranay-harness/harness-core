/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.rest.RestResponse.Builder.aRestResponse;

import io.harness.rest.RestResponse;

import software.wings.beans.AccountPlugin;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.PluginService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
