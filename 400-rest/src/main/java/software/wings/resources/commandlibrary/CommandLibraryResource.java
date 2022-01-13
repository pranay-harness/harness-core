/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.commandlibrary;

import static io.harness.rest.RestResponse.Builder.aRestResponse;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import io.harness.rest.RestResponse;

import software.wings.api.commandlibrary.CommandLibraryConfigurationDTO;
import software.wings.beans.template.Template;
import software.wings.beans.template.dto.ImportedCommand;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("command-library")
@Path("/command-library")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
public class CommandLibraryResource {
  @Inject TemplateVersionService templateVersionService;
  @Inject ImportedTemplateService importedTemplateService;

  @GET
  @Path("/configuration")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandLibraryConfigurationDTO> getCommandStores(@QueryParam("accountId") String accountId) {
    return aRestResponse()
        .withResource(CommandLibraryConfigurationDTO.builder()
                          .clImplementationVersion(1)
                          .supportedCommandStoreNameList(Collections.singletonList("harness"))
                          .build())
        .build();
  }

  @GET
  @Path("command-stores/{commandStoreName}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<ImportedCommand>> listImportedCommandVersions(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId,
      @QueryParam("commandNames") List<String> commandNames, @PathParam("commandStoreName") String commandStoreName) {
    return new RestResponse<>(
        templateVersionService.listLatestVersionOfImportedTemplates(commandNames, commandStoreName, accountId, appId));
  }

  @GET
  @Path("command-stores/{commandStoreName}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<ImportedCommand> listLatestVersionsOfTemplates(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("commandName") String commandName,
      @PathParam("commandStoreName") String commandStoreName) {
    return new RestResponse<>(
        templateVersionService.listImportedTemplateVersions(commandName, commandStoreName, accountId, appId));
  }

  @POST
  @Path("command-stores/{commandStoreName}/commands/{commandName}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<Template> importTemplate(@QueryParam("accountId") String accountId,
      @DefaultValue(GLOBAL_APP_ID) @QueryParam("appId") String appId, @PathParam("commandName") String commandName,
      @PathParam("commandStoreName") String commandStoreName, @PathParam("version") String version) {
    return new RestResponse<>(
        importedTemplateService.getAndSaveImportedTemplate(version, commandName, commandStoreName, accountId, appId));
  }
}
