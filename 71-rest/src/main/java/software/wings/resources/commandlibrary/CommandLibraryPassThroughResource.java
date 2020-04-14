package software.wings.resources.commandlibrary;

import static io.harness.commandlibrary.client.CommandLibraryServiceClientUtils.executeHttpRequest;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.Call;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.HarnessUserGroupServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Api("command-library-service")
@Path("/command-library-service")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
public class CommandLibraryPassThroughResource {
  private final CommandLibraryServiceHttpClient serviceHttpClient;
  private final HarnessUserGroupServiceImpl harnessUserGroupService;

  @Inject
  public CommandLibraryPassThroughResource(
      CommandLibraryServiceHttpClient serviceHttpClient, HarnessUserGroupServiceImpl harnessUserGroupService) {
    this.serviceHttpClient = serviceHttpClient;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Path("/command-stores")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<CommandStoreDTO>> getCommandStores(@QueryParam("accountId") String accountId) {
    return executeRequest(serviceHttpClient.getCommandStores());
  }

  @GET
  @Path("/command-stores/{commandStoreId}/commands/categories")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<String>> getCommandCategories(
      @QueryParam("accountId") String accountId, @PathParam("commandStoreId") String commandStoreId) {
    return executeRequest(serviceHttpClient.getCommandCategories(commandStoreId));
  }

  @GET
  @Path("/command-stores/{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<PageResponse<CommandDTO>> listCommands(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @BeanParam PageRequest<CommandEntity> pageRequest,
      @QueryParam("cl_implementation_version") Integer clImplementationVersion, @QueryParam("category") String category,
      @Context UriInfo uriInfo) {
    Map<String, Object> queryMap = prepareQueryMap(uriInfo.getQueryParameters());
    return executeRequest(serviceHttpClient.listCommands(commandStoreId, clImplementationVersion, category, queryMap));
  }

  private Map<String, Object> prepareQueryMap(Map<String, List<String>> queryParameters) {
    return queryParameters.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
  }

  @GET
  @Path("/command-stores/{commandStoreId}/commands/{commandId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandDTO> getCommandDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId) {
    return executeRequest(serviceHttpClient.getCommandDetails(commandStoreId, commandId));
  }

  @POST
  @Path("/command-stores/{commandStoreId}/commands")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandEntity> saveCommand(@QueryParam("accountId") String accountId, CommandEntity commandEntity,
      @PathParam("commandStoreId") String commandStoreId) {
    ensureHarnessUser();
    return executeRequest(serviceHttpClient.saveCommand(commandStoreId, commandEntity));
  }

  private void ensureHarnessUser() {
    if (!harnessUserGroupService.isHarnessSupportUser(UserThreadLocal.get().getUuid())) {
      throw new UnauthorizedException("You don't have the permissions to perform this action.", WingsException.USER);
    }
  }

  @GET
  @Path("/command-stores/{commandStoreId}/commands/{commandId}/versions/{version}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<EnrichedCommandVersionDTO> getVersionDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId,
      @PathParam("version") String version) {
    return executeRequest(serviceHttpClient.getVersionDetails(commandStoreId, commandId, version));
  }

  @POST
  @Path("/command-stores/{commandStoreId}/commands/{commandId}/versions")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CommandVersionEntity> saveCommandVersion(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreId") String commandStoreId, @PathParam("commandId") String commandId,
      CommandVersionEntity commandVersionEntity) {
    ensureHarnessUser();
    return executeRequest(serviceHttpClient.saveCommandVersion(commandStoreId, commandId, commandVersionEntity));
  }

  private <T> T executeRequest(Call<T> call) {
    return executeHttpRequest(call);
  }
}