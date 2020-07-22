package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("secrets")
@Api("secrets")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretResource {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private final NGSecretService ngSecretService;

  @POST
  @ApiOperation(value = "Create a secret text", nickname = "createSecretText")
  public ResponseDTO<String> create(
      @QueryParam("local") @DefaultValue("false") boolean localMode, SecretTextDTO secretText) {
    return ResponseDTO.newResponse(ngSecretService.createSecret(localMode, secretText));
  }

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecrets")
  public ResponseDTO<List<EncryptedDataDTO>> list(@QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier,
      @QueryParam("type") SecretType secretType) {
    List<EncryptedDataDTO> secrets =
        ngSecretService.listSecrets(accountIdentifier, orgIdentifier, projectIdentifier, secretType);
    return ResponseDTO.newResponse(secrets);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecretText")
  public ResponseDTO<EncryptedDataDTO> get(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    EncryptedDataDTO encryptedData =
        ngSecretService.getSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(encryptedData);
  }

  @PUT
  @ApiOperation(value = "Update a secret text", nickname = "updateSecretText")
  public ResponseDTO<Boolean> updateSecret(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier,
      @Body @Valid SecretTextUpdateDTO dto) {
    return ResponseDTO.newResponse(
        ngSecretService.updateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto));
  }

  @DELETE
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecretText")
  public ResponseDTO<Boolean> deleteSecret(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.deleteSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
