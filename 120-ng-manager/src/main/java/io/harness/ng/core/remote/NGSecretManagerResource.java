package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
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
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String IDENTIFIER = "identifier";
  @Inject private final NGSecretManagerService ngSecretManagerService;

  @POST
  @ApiOperation(value = "Create a secret manager", nickname = "createSecretManager")
  public ResponseDTO<String> create(NGSecretManagerConfigDTO secretManagerConfig) {
    return ResponseDTO.newResponse(ngSecretManagerService.createSecretManager(secretManagerConfig));
  }

  @GET
  @ApiOperation(value = "Get secret managers", nickname = "listSecretManagers")
  public ResponseDTO<List<NGSecretManagerConfigDTO>> list(
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    List<NGSecretManagerConfigDTO> secretManagerConfigs =
        ngSecretManagerService.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(secretManagerConfigs);
  }

  @GET
  @Path("/{identifier}")
  @ApiOperation(value = "Get a secret manager by identifier", nickname = "getSecretManagerByIdentifier")
  public ResponseDTO<NGSecretManagerConfigDTO> get(@PathParam(IDENTIFIER) String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    NGSecretManagerConfigDTO secretManagerConfig =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(secretManagerConfig);
  }

  @PUT
  @Path("/{identifier}")
  @ApiOperation(value = "Update secret manager", nickname = "updateSecretManager")
  public ResponseDTO<String> update(@PathParam(IDENTIFIER) String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier,
      NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    return ResponseDTO.newResponse(ngSecretManagerService.updateSecretManager(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, secretManagerConfigUpdateDTO));
  }

  @DELETE
  @Path("/{identifier}")
  @ApiOperation(value = "Delete secret manager", nickname = "deleteSecretManager")
  public ResponseDTO<Boolean> delete(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretManagerService.deleteSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}