package software.wings.resources.commandlibrary;

import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.api.commandlibrary.CommandLibraryConfigurationDTO;
import software.wings.beans.template.Template;
import software.wings.beans.template.dto.ImportedCommand;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import java.util.Collections;
import java.util.List;
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
                          .supportedCommandStoreIdList(Collections.singletonList("harness"))
                          .build())
        .build();
  }

  @GET
  @Path("command-stores/{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<ImportedCommand>> listImportedCommandVersions(@QueryParam("accountId") String accountId,
      @QueryParam("commandIds") List<String> commandIds, @PathParam("commandStoreId") String commandStoreId) {
    return new RestResponse<>(
        templateVersionService.listLatestVersionOfImportedTemplates(commandIds, commandStoreId, accountId));
  }

  @GET
  @Path("command-stores/{commandStoreId}/commands/{commandId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<ImportedCommand> listLatestVersionsOfTemplates(@QueryParam("accountId") String accountId,
      @PathParam("commandId") String commandId, @PathParam("commandStoreId") String commandStoreId) {
    return new RestResponse<>(
        templateVersionService.listImportedTemplateVersions(commandId, commandStoreId, accountId));
  }

  @POST
  @Path("command-stores/{commandStoreId}/commands/{commandId}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = TEMPLATE_MANAGEMENT)
  public RestResponse<Template> importTemplate(@QueryParam("accountId") String accountId,
      @PathParam("commandId") String commandId, @PathParam("commandStoreId") String commandStoreId,
      @PathParam("version") String version) {
    return new RestResponse<>(
        importedTemplateService.getAndSaveImportedTemplate(version, commandId, commandStoreId, accountId));
  }
  // Add api to get specific version of a command.
}
