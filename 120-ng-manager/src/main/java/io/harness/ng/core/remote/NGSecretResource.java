package io.harness.ng.core.remote;

import static io.harness.secretmanagerclient.NGConstants.ACCOUNT_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.ORG_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.PROJECT_IDENTIFIER_KEY;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import com.google.inject.Inject;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.security.encryption.EncryptedData;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
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

  @POST
  @ApiOperation(value = "Create a secret text", nickname = "createSecretText")
  public ResponseDTO<EncryptedDataDTO> create(SecretTextCreateDTO secretText) {
    return ResponseDTO.newResponse(toDTO(ngSecretService.create(secretText)));
  }

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecrets")
  public ResponseDTO<List<EncryptedDataDTO>> list(@QueryParam(ACCOUNT_IDENTIFIER_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier, @QueryParam("type") SecretType secretType,
      @QueryParam("searchTerm") String searchTerm) {
    List<EncryptedData> secrets =
        ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier, secretType, searchTerm);
    return ResponseDTO.newResponse(secrets.stream().map(EncryptedDataMapper::toDTO).collect(Collectors.toList()));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecretText")
  public ResponseDTO<EncryptedDataDTO> get(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier) {
    EncryptedData encryptedData = ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(toDTO(encryptedData));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret text", nickname = "updateSecretText")
  public ResponseDTO<Boolean> updateSecret(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier, @Body @Valid SecretTextUpdateDTO dto) {
    return ResponseDTO.newResponse(
        ngSecretService.update(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecretText")
  public ResponseDTO<Boolean> deleteSecret(@PathParam("identifier") @NotEmpty String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
