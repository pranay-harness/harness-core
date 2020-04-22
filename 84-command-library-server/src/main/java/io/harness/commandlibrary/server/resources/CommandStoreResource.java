package io.harness.commandlibrary.server.resources;

import static io.harness.rest.RestResponse.Builder.aRestResponse;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.commandlibrary.server.common.CommandVersionUtils;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandStoreService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("command-stores")
@Path("/command-stores")
@Produces("application/json")
public class CommandStoreResource {
  public static final String HARNESS = "harness";

  @Inject private CommandStoreService commandStoreService;
  @Inject private CommandService commandService;
  @Inject private CommandVersionService commandVersionService;

  @GET
  @Path("test")
  @PublicApi
  public RestResponse<String> testApi() {
    return new RestResponse<>("hello world");
  }

  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<CommandStoreDTO>> getCommandStores(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(Collections.singletonList(CommandStoreDTO.builder()
                                                            .name(HARNESS)
                                                            .description("Harness Command Library")
                                                            .displayName("Harness Inc")
                                                            .build()));
  }

  @GET
  @Path("{commandStoreName}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<String>> getCommandCategories(
      @QueryParam("accountId") String accountId, @PathParam("commandStoreName") String commandStoreName) {
    return new RestResponse<>(Arrays.asList("Kubernetes", "Azure", "GCP"));
  }

  @GET
  @Path("{commandStoreName}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<PageResponse<CommandDTO>> listCommands(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @BeanParam PageRequest<CommandEntity> pageRequest,
      @QueryParam("cl_implementation_version") Integer clImplementationVersion,
      @QueryParam("category") String category) {
    return aRestResponse()
        .withResource(commandStoreService.listCommandsForStore(commandStoreName, pageRequest, category))
        .build();
  }

  @GET
  @Path("{commandStoreName}/commands/{commandName}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandDTO> getCommandDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @PathParam("commandName") String commandName) {
    return aRestResponse().withResource(commandService.getCommandDetails(commandStoreName, commandName)).build();
  }

  @POST
  @Path("{commandStoreName}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandEntity> saveCommand(
      @QueryParam("accountId") String accountId, CommandEntity commandEntity) {
    final String commandId = commandService.save(commandEntity);
    return aRestResponse().withResource(commandService.getEntityById(commandId)).build();
  }

  @GET
  @Path("{commandStoreName}/commands/{commandName}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<EnrichedCommandVersionDTO> getVersionDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @PathParam("commandName") String commandName,
      @PathParam("version") String version) {
    final CommandVersionEntity commandVersionEntity =
        commandVersionService.getCommandVersionEntity(commandStoreName, commandName, version);

    return aRestResponse()
        .withResource(
            CommandVersionUtils.populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), commandVersionEntity)
                .build())
        .build();
  }

  @POST
  @Path("{commandStoreName}/commands/{commandName}/versions")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandVersionEntity> saveCommandVersion(
      @QueryParam("accountId") String accountId, CommandVersionEntity commandVersionEntity) {
    final String commandVersionId = commandVersionService.save(commandVersionEntity);
    return aRestResponse().withResource(commandVersionService.getEntityById(commandVersionId)).build();
  }
}
