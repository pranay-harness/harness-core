package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.encryption.SecretType;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.SampleEncryptableSettingImplementation;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
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
  private final NGSecretService ngSecretService;
  private final SecretManagerClientService secretManagerClientService;

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecretsForAccount")
  public ResponseDTO<List<EncryptedDataDTO>> getSecretsForAccount(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier, @QueryParam("type") SecretType secretType) {
    List<EncryptedDataDTO> secrets = ngSecretService.getSecretsByType(accountIdentifier, secretType);
    return ResponseDTO.newResponse(secrets);
  }

  @GET
  @Path("{secretId}")
  @ApiOperation(value = "Gets a secret by id", nickname = "getSecretById")
  public ResponseDTO<EncryptedDataDTO> get(@PathParam("secretId") @NotEmpty String secretId,
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    EncryptedDataDTO encryptedData = ngSecretService.getSecretById(accountIdentifier, secretId);
    return ResponseDTO.newResponse(encryptedData);
  }

  @POST
  @ApiOperation(value = "Create a secret text", nickname = "createSecretText")
  public ResponseDTO<String> createSecret(@QueryParam("accountId") String accountId,
      @QueryParam("local") @DefaultValue("false") boolean localMode, SecretTextDTO secretText) {
    return ResponseDTO.newResponse(ngSecretService.createSecret(accountId, localMode, secretText));
  }

  @PUT
  @ApiOperation(value = "Update a secret text", nickname = "updateSecretText")
  public ResponseDTO<Boolean> updateSecret(@QueryParam("accountId") @NotBlank final String accountId,
      @QueryParam("uuid") @NotBlank final String uuid, @Body @Valid SecretTextDTO secretText) {
    return ResponseDTO.newResponse(ngSecretService.updateSecret(accountId, uuid, secretText));
  }

  @DELETE
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecretText")
  public ResponseDTO<Boolean> deleteSecret(
      @QueryParam("accountId") @NotBlank String accountId, @QueryParam("uuid") @NotBlank String uuId) {
    return ResponseDTO.newResponse(ngSecretService.deleteSecret(accountId, uuId));
  }

  @GET
  @Path("test")
  @ApiOperation(value = "test", nickname = "test")
  public ResponseDTO<List<EncryptedDataDetail>> testMethod() {
    SampleEncryptableSettingImplementation test =
        SampleEncryptableSettingImplementation.builder().encryptedSecretText("5gCyTeToQFacz8Y9bXGRyw").build();
    return ResponseDTO.newResponse(secretManagerClientService.getEncryptionDetails(test));
  }
}
