package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
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
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Path("/v2/secrets")
@Api("/v2/secrets")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
public class NGSecretResourceV2 {
  private static final String INCLUDE_SECRETS_FROM_EVERY_SUB_SCOPE = "includeSecretsFromEverySubScope";
  private final SecretCrudService ngSecretService;
  private final Validator validator;
  private final SecretManagerClientService secretManagerClientService;

  @GET
  @Path("/validateUniqueIdentifier/{identifier}")
  @ApiOperation(value = "Validate Secret Identifier is unique", nickname = "validateSecretIdentifierIsUnique")
  public ResponseDTO<Boolean> validateTheIdentifierIsUnique(
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.validateTheIdentifierIsUnique(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @POST
  @Consumes({"application/json"})
  @ApiOperation(value = "Create a secret", nickname = "postSecret")
  public ResponseDTO<SecretResponseWrapper> create(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Valid @NotNull SecretRequestWrapper dto) {
    return ResponseDTO.newResponse(ngSecretService.create(accountIdentifier, dto.getSecret()));
  }

  @POST
  @Path("/validate")
  @Consumes({"application/json", "application/yaml"})
  @ApiOperation(value = "Validate a secret", nickname = "validateSecret")
  public ResponseDTO<SecretValidationResultDTO> validateSecret(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier, @Valid SecretValidationMetaData metadata) {
    return ResponseDTO.newResponse(
        ngSecretService.validateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, metadata));
  }

  @POST
  @Path("/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Create a secret via yaml", nickname = "postSecretViaYaml")
  public ResponseDTO<SecretResponseWrapper> createViaYaml(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Valid SecretRequestWrapper dto) {
    return ResponseDTO.newResponse(ngSecretService.createViaYaml(accountIdentifier, dto.getSecret()));
  }

  @GET
  @ApiOperation(value = "Get secrets", nickname = "listSecretsV2")
  public ResponseDTO<PageResponse<SecretResponseWrapper>> list(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers,
      @QueryParam("type") SecretType secretType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("types") List<SecretType> secretTypes,
      @QueryParam(INCLUDE_SECRETS_FROM_EVERY_SUB_SCOPE) @DefaultValue("false") boolean includeSecretsFromEverySubScope,
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size) {
    if (secretType != null) {
      secretTypes.add(secretType);
    }
    return ResponseDTO.newResponse(ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier,
        identifiers, secretTypes, includeSecretsFromEverySubScope, searchTerm, page, size));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecretV2")
  public ResponseDTO<SecretResponseWrapper> get(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete secret", nickname = "deleteSecretV2")
  public ResponseDTO<Boolean> delete(@PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret", nickname = "putSecret")
  @Consumes({"application/json"})
  public ResponseDTO<SecretResponseWrapper> updateSecret(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Valid SecretRequestWrapper dto) {
    return ResponseDTO.newResponse(
        ngSecretService.update(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto.getSecret()));
  }

  @PUT
  @Path("{identifier}/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Update a secret via yaml", nickname = "putSecretViaYaml")
  public ResponseDTO<SecretResponseWrapper> updateSecretViaYaml(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Valid SecretRequestWrapper dto) {
    return ResponseDTO.newResponse(ngSecretService.updateViaYaml(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto.getSecret()));
  }

  private void validateRequestPayload(SecretRequestWrapper dto) {
    Set<ConstraintViolation<SecretRequestWrapper>> violations = validator.validate(dto);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  @PUT
  @Path("files/{identifier}")
  @ApiOperation(value = "Update a secret file", nickname = "putSecretFileV2")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<SecretResponseWrapper> updateSecretFile(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("spec") String spec) {
    SecretRequestWrapper dto = JsonUtils.asObject(spec, SecretRequestWrapper.class);

    validateRequestPayload(dto);

    return ResponseDTO.newResponse(ngSecretService.updateFile(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto.getSecret(), uploadedInputStream));
  }

  @POST
  @Path("files")
  @ApiOperation(value = "Create a secret file", nickname = "postSecretFileV2")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<SecretResponseWrapper> createSecretFile(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @NotNull @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("spec") String spec) {
    SecretRequestWrapper dto = JsonUtils.asObject(spec, SecretRequestWrapper.class);

    validateRequestPayload(dto);

    return ResponseDTO.newResponse(ngSecretService.createFile(accountIdentifier, dto.getSecret(), uploadedInputStream));
  }

  @POST
  @Path("encryption-details")
  @ApiOperation(hidden = true, value = "Get Encryption Details", nickname = "postEncryptionDetails")
  @InternalApi
  public ResponseDTO<List<EncryptedDataDetail>> getEncryptionDetails(
      NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer) {
    return ResponseDTO.newResponse(secretManagerClientService.getEncryptionDetails(
        ngAccessWithEncryptionConsumer.getNgAccess(), ngAccessWithEncryptionConsumer.getDecryptableEntity()));
  }
}
