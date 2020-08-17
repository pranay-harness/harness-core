package io.harness.ng.core.remote;

import static io.harness.secretmanagerclient.NGConstants.ACCOUNT_KEY;
import static io.harness.secretmanagerclient.NGConstants.IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.ORG_KEY;
import static io.harness.secretmanagerclient.NGConstants.PROJECT_KEY;

import com.google.inject.Inject;

import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
  @ApiOperation(value = "Create a secret manager", nickname = "createSecretManager")
  public ResponseDTO<SecretManagerConfigDTO> create(SecretManagerConfigDTO secretManagerConfig) {
    return ResponseDTO.newResponse(ngSecretManagerService.createSecretManager(secretManagerConfig));
  }

  @GET
  @ApiOperation(value = "Get secret managers", nickname = "listSecretManagers")
  public ResponseDTO<List<SecretManagerConfigDTO>> list(@QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier) {
    List<SecretManagerConfigDTO> secretManagerConfigs =
        ngSecretManagerService.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(secretManagerConfigs);
  }

  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get a secret manager by identifier", nickname = "getSecretManagerByIdentifier")
  public ResponseDTO<SecretManagerConfigDTO> get(@PathParam(IDENTIFIER_KEY) String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    SecretManagerConfigDTO secretManagerConfig =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(secretManagerConfig);
  }

  @PUT
  @Path("/{identifier}")
  @ApiOperation(value = "Update secret manager", nickname = "updateSecretManager")
  public ResponseDTO<SecretManagerConfigDTO> update(@PathParam(IDENTIFIER_KEY) String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    return ResponseDTO.newResponse(ngSecretManagerService.updateSecretManager(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, secretManagerConfigUpdateDTO));
  }

  @DELETE
  @Path("/{identifier}")
  @ApiOperation(value = "Delete secret manager", nickname = "deleteSecretManager")
  public ResponseDTO<Boolean> delete(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretManagerService.deleteSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}