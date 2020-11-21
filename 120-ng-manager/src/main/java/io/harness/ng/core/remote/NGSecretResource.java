package io.harness.ng.core.remote;

import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.EncryptedData;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.NGSecretService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;

import software.wings.resources.secretsmanagement.EncryptedDataMapper;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

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
  public ResponseDTO<PageResponse<EncryptedDataDTO>> list(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("type") SecretType secretType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size) {
    PageResponse<EncryptedData> secrets =
        ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier, secretType, searchTerm, page, size);
    PageResponse<EncryptedDataDTO> encryptedDataDTOPageResponse = PageResponse.<EncryptedDataDTO>builder()
                                                                      .empty(secrets.isEmpty())
                                                                      .pageIndex(secrets.getPageIndex())
                                                                      .pageSize(secrets.getPageSize())
                                                                      .totalItems(secrets.getTotalItems())
                                                                      .totalPages(secrets.getTotalPages())
                                                                      .pageItemCount(secrets.getPageItemCount())
                                                                      .build();
    encryptedDataDTOPageResponse.setContent(
        secrets.getContent().stream().map(EncryptedDataMapper::toDTO).collect(Collectors.toList()));

    return ResponseDTO.newResponse(encryptedDataDTOPageResponse);
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecret")
  public ResponseDTO<EncryptedDataDTO> get(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    EncryptedData encryptedData = ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(toDTO(encryptedData));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret text", nickname = "putSecretText")
  @Consumes({"application/json"})
  public ResponseDTO<Boolean> updateSecret(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(ngSecretService.update(dto, false));
  }

  @PUT
  @Path("{identifier}/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Update a secret text via yaml", nickname = "putSecretTextViaYaml")
  public ResponseDTO<Boolean> updateSecretViaYaml(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretTextDTO dto) {
    return ResponseDTO.newResponse(ngSecretService.update(dto, true));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a secret text", nickname = "deleteSecret")
  public ResponseDTO<Boolean> deleteSecret(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
