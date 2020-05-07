package software.wings.resources.commandlibrary;

import static io.harness.commandlibrary.client.CommandLibraryServiceClientUtils.executeAndCreatePassThroughResponse;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.exception.GeneralException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import retrofit2.Call;
import software.wings.app.MainConfiguration;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.HarnessUserGroupServiceImpl;
import software.wings.service.intfc.HarnessUserGroupService;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Api("command-library-service")
@Path("/command-library-service")
@Produces("application/json")
@AuthRule(permissionType = LOGGED_IN)
public class CommandLibraryServiceResource {
  private final CommandLibraryServiceHttpClient serviceHttpClient;
  private final HarnessUserGroupService harnessUserGroupService;
  private final MainConfiguration mainConfiguration;

  @Inject
  public CommandLibraryServiceResource(CommandLibraryServiceHttpClient serviceHttpClient,
      HarnessUserGroupServiceImpl harnessUserGroupService, MainConfiguration mainConfiguration) {
    this.serviceHttpClient = serviceHttpClient;
    this.harnessUserGroupService = harnessUserGroupService;
    this.mainConfiguration = mainConfiguration;
  }

  @GET
  @Path("/command-stores")
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response getCommandStores(@QueryParam("accountId") String accountId, @Context UriInfo uriInfo) {
    return executeRequest(serviceHttpClient.getCommandStores(prepareQueryMap(uriInfo.getQueryParameters())));
  }

  @GET
  @Path("/command-stores/{commandStoreName}/commands/tags")
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response getCommandTags(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @Context UriInfo uriInfo) {
    return executeRequest(
        serviceHttpClient.getCommandTags(commandStoreName, prepareQueryMap(uriInfo.getQueryParameters())));
  }

  @GET
  @Path("/command-stores/{commandStoreName}/commands")
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response listCommands(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @BeanParam PageRequest<CommandEntity> pageRequest,
      @Context UriInfo uriInfo) {
    Map<String, Object> queryMap = prepareQueryMap(uriInfo.getQueryParameters());
    return executeRequest(serviceHttpClient.listCommands(commandStoreName, queryMap));
  }

  @GET
  @Path("/command-stores/{commandStoreName}/commands/{commandName}")
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response getCommandDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @PathParam("commandName") String commandName,
      @Context UriInfo uriInfo) {
    return executeRequest(serviceHttpClient.getCommandDetails(
        commandStoreName, commandName, prepareQueryMap(uriInfo.getQueryParameters())));
  }

  private void ensureHarnessUser() {
    if (!harnessUserGroupService.isHarnessSupportUser(UserThreadLocal.get().getUuid())) {
      throw new UnauthorizedException("You don't have the permissions to perform this action.", WingsException.USER);
    }
  }

  @GET
  @Path("/command-stores/{commandStoreName}/commands/{commandName}/versions/{version}")
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response getVersionDetails(@QueryParam("accountId") String accountId,
      @PathParam("commandStoreName") String commandStoreName, @PathParam("commandName") String commandName,
      @PathParam("version") String version, @Context UriInfo uriInfo) {
    return executeRequest(serviceHttpClient.getVersionDetails(
        commandStoreName, commandName, version, prepareQueryMap(uriInfo.getQueryParameters())));
  }

  @POST
  @Path("/command-stores/{commandStoreName}/commands")
  @Consumes(MULTIPART_FORM_DATA)
  @Produces(APPLICATION_JSON)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public Response publishCommand(@QueryParam("accountId") final String accountId,
      @PathParam("commandStoreName") String commandStoreName,
      @FormDataParam("file") final InputStream uploadInputStream, @Context UriInfo uriInfo) {
    try {
      ensureHarnessUser();
      final byte[] bytes = IOUtils.toByteArray(
          new BoundedInputStream(uploadInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
      final MultipartBody.Part formData =
          MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      return executeRequest(
          serviceHttpClient.publishCommand(commandStoreName, formData, prepareQueryMap(uriInfo.getQueryParameters())));
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      throw new GeneralException("Error while publishing command", e);
    }
  }

  private <T> Response executeRequest(Call<T> call) {
    return executeAndCreatePassThroughResponse(call);
  }

  private Map<String, Object> prepareQueryMap(Map<String, List<String>> queryParameters) {
    return queryParameters.entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
  }
}