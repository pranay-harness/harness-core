package io.harness.ng.core.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.ORG_KEY;
import static io.harness.ng.NGConstants.PAGE_KEY;
import static io.harness.ng.NGConstants.PROJECT_KEY;
import static io.harness.ng.NGConstants.SEARCH_TERM_KEY;
import static io.harness.ng.NGConstants.SIZE_KEY;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.api.NGSecretService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.security.encryption.EncryptedData;

import java.util.stream.Collectors;
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
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretResource {
  private final NGSecretService ngSecretService;

  @POST
  @Consumes({"application/json"})
  @ApiOperation(value = "Create a secret text", nickname = "postSecretText")
  public ResponseDTO<EncryptedDataDTO> createViaJson(@Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(toDTO(ngSecretService.create(dto, false)));
  }

  @POST
  @Path("/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Create a secret text via yaml", nickname = "postSecretTextViaYaml")
  public ResponseDTO<EncryptedDataDTO> createViaYaml(@Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(toDTO(ngSecretService.create(dto, true)));
  }

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecrets")
  public ResponseDTO<NGPageResponse<EncryptedDataDTO>> list(@QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam("type") SecretType secretType, @QueryParam(SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(PAGE_KEY) @DefaultValue("0") int page, @QueryParam(SIZE_KEY) @DefaultValue("100") int size) {
    NGPageResponse<EncryptedData> secrets =
        ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier, secretType, searchTerm, page, size);
    NGPageResponse<EncryptedDataDTO> encryptedDataDTOPageResponse = NGPageResponse.<EncryptedDataDTO>builder()
                                                                        .empty(secrets.isEmpty())
                                                                        .pageIndex(secrets.getPageIndex())
                                                                        .pageSize(secrets.getPageSize())
                                                                        .itemCount(secrets.getItemCount())
                                                                        .pageCount(secrets.getPageCount())
                                                                        .build();
    encryptedDataDTOPageResponse.setContent(
        secrets.getContent().stream().map(EncryptedDataMapper::toDTO).collect(Collectors.toList()));

    return ResponseDTO.newResponse(encryptedDataDTOPageResponse);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecret")
  public ResponseDTO<EncryptedDataDTO> get(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    EncryptedData encryptedData = ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(toDTO(encryptedData));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret text", nickname = "putSecretText")
  @Consumes({"application/json"})
  public ResponseDTO<Boolean> updateSecret(
      @PathParam(IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(ngSecretService.update(dto, false));
  }

  @PUT
  @Path("{identifier}/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Update a secret text via yaml", nickname = "putSecretTextViaYaml")
  public ResponseDTO<Boolean> updateSecretViaYaml(
      @PathParam(IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(ngSecretService.update(dto, true));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecret")
  public ResponseDTO<Boolean> deleteSecret(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
