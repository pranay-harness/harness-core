package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.EncryptedDataMapper.writeDTO;

import com.google.inject.Inject;

import io.harness.ng.core.ErrorDTO;
import io.harness.ng.core.FailureDTO;
import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.dto.EncryptedDataDTO;
import io.harness.ng.core.services.api.NGSecretService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
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

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecretsForAccount")
  public ResponseDTO<List<EncryptedDataDTO>> getSecretsForAccount(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("type") @NotNull SettingVariableTypes type,
      @QueryParam("includeDetails") @DefaultValue("true") boolean details) {
    List<EncryptedData> secrets = ngSecretService.getSecretsByType(accountIdentifier, type, details);
    List<EncryptedDataDTO> secretsDto = new ArrayList<>();
    secrets.forEach(secret -> secretsDto.add(writeDTO(secret)));
    return ResponseDTO.newResponse(secretsDto);
  }

  @GET
  @Path("{secretId}")
  @ApiOperation(value = "Gets a secret by id", nickname = "getSecretById")
  public ResponseDTO<EncryptedDataDTO> get(@PathParam("secretId") @NotEmpty String secretId,
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    EncryptedData encryptedData = ngSecretService.getSecretById(accountIdentifier, secretId);
    return ResponseDTO.newResponse(writeDTO(encryptedData));
  }

  @POST
  @ApiOperation(value = "Create a secret text", nickname = "createSecretText")
  public ResponseDTO<String> createSecret(@QueryParam("accountId") String accountId,
      @QueryParam("local") @DefaultValue("false") boolean localMode, SecretText secretText) {
    return ResponseDTO.newResponse(ngSecretService.createSecret(accountId, localMode, secretText));
  }

  @PUT
  @ApiOperation(value = "Update a secret text", nickname = "updateSecretText")
  public ResponseDTO<Boolean> updateSecret(@QueryParam("accountId") @NotBlank final String accountId,
      @QueryParam("uuid") @NotBlank final String uuid, @Body @Valid SecretText secretText) {
    return ResponseDTO.newResponse(ngSecretService.updateSecret(accountId, uuid, secretText));
  }

  @DELETE
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecretText")
  public ResponseDTO<Boolean> deleteSecret(
      @QueryParam("accountId") @NotBlank String accountId, @QueryParam("uuid") @NotBlank String uuId) {
    return ResponseDTO.newResponse(ngSecretService.deleteSecret(accountId, uuId));
  }
}
