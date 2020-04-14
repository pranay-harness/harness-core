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
    return new RestResponse<>(Collections.singletonList(
        CommandStoreDTO.builder().id(HARNESS).description("Harness Command Library").name("Harness Inc").build()));
  }

  @GET
  @Path("{commandStoreId}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<String>> getCommandCategories(
      @QueryParam("accountId") String accountId, @PathParam("commandStoreId") String commandStoreId) {
    return new RestResponse<>(Arrays.asList("Kubernetes", "Azure", "GCP"));
  }

  @GET
  @Path("{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<PageResponse<CommandDTO>> listCommands(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @BeanParam PageRequest<CommandEntity> pageRequest,
      @QueryParam("cl_implementation_version") Integer clImplementationVersion,
      @QueryParam("category") String category) {
    return aRestResponse()
        .withResource(commandStoreService.listCommandsForStore(commandStoreId, pageRequest, category))
        .build();
  }

  @GET
  @Path("{commandStoreId}/commands/{commandId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandDTO> getCommandDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId) {
    return aRestResponse().withResource(commandService.getCommandDetails(commandStoreId, commandId)).build();
  }

  @POST
  @Path("{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandEntity> saveCommand(
      @QueryParam("accountId") String accountId, CommandEntity commandEntity) {
    final String commandId = commandService.save(commandEntity);
    return aRestResponse().withResource(commandService.getEntityById(commandId)).build();
  }

  @GET
  @Path("{commandStoreId}/commands/{commandId}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<EnrichedCommandVersionDTO> getVersionDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId,
      @PathParam("version") String version) {
    final CommandVersionEntity commandVersionEntity =
        commandVersionService.getCommandVersionEntity(commandStoreId, commandId, version);

    return aRestResponse()
        .withResource(
            CommandVersionUtils.populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), commandVersionEntity)
                .build())
        .build();
  }

  @POST
  @Path("{commandStoreId}/commands/{commandId}/versions")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandVersionEntity> saveCommandVersion(
      @QueryParam("accountId") String accountId, CommandVersionEntity commandVersionEntity) {
    final String commandVersionId = commandVersionService.save(commandVersionEntity);
    return aRestResponse().withResource(commandVersionService.getEntityById(commandVersionId)).build();
  }
}
