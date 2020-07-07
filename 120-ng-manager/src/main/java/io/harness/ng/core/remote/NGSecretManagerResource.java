package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.ErrorDTO;
import io.harness.ng.core.FailureDTO;
import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import software.wings.beans.SecretManagerConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secret-managers")
@Path("secret-managers")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class NGSecretManagerResource {
  @Inject private final NGSecretManagerService ngSecretManagerService;

  @POST
  @ApiOperation(value = "Create or update a secret manager", nickname = "saveOrUpdateSecretManager")
  public ResponseDTO<String> saveOrUpdateSecretManager(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier, SecretManagerConfig secretManagerConfig) {
    return ResponseDTO.newResponse(
        ngSecretManagerService.saveOrUpdateSecretManager(accountIdentifier, secretManagerConfig));
  }

  @GET
  @ApiOperation(value = "Get secret managers for an account", nickname = "listSecretManagers")
  public ResponseDTO<List<SecretManagerConfig>> listSecretManagersForAccount(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    return ResponseDTO.newResponse(ngSecretManagerService.listSecretManagers(accountIdentifier));
  }

  @GET
  @Path("/{kmsId}")
  @ApiOperation(value = "Get a secret manager by kmsId", nickname = "getSecretManagerById")
  public ResponseDTO<SecretManagerConfig> getNgSecretManagerService(
      @PathParam("kmsId") String kmsId, @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    return ResponseDTO.newResponse(ngSecretManagerService.getSecretManager(accountIdentifier, kmsId));
  }

  @DELETE
  @Path("/{kmsId}")
  @ApiOperation(value = "Delete secret manager", nickname = "deleteSecretManager")
  public ResponseDTO<Boolean> deleteSecretManager(
      @PathParam("kmsId") String kmsId, @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    return ResponseDTO.newResponse(ngSecretManagerService.deleteSecretManager(accountIdentifier, kmsId));
  }
}
