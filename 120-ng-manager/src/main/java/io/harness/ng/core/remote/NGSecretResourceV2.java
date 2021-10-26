package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.secrets.SecretPermissions.SECRET_ACCESS_PERMISSION;
import static io.harness.secrets.SecretPermissions.SECRET_DELETE_PERMISSION;
import static io.harness.secrets.SecretPermissions.SECRET_EDIT_PERMISSION;
import static io.harness.secrets.SecretPermissions.SECRET_RESOURCE_TYPE;
import static io.harness.secrets.SecretPermissions.SECRET_VIEW_PERMISSION;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorCategory;
import io.harness.data.validator.EntityIdentifier;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.NGAccessWithEncryptionConsumer;
import io.harness.ng.core.api.NGEncryptedDataService;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.api.impl.SecretPermissionValidator;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.SecretResourceFilterDTO;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.secretmanagerclient.SecretType;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;

import software.wings.service.impl.security.NGEncryptorService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

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
@Slf4j
public class NGSecretResourceV2 {
  private static final String INCLUDE_SECRETS_FROM_EVERY_SUB_SCOPE = "includeSecretsFromEverySubScope";
  private final SecretCrudService ngSecretService;
  private final Validator validator;
  private final NGEncryptedDataService encryptedDataService;
  private final SecretPermissionValidator secretPermissionValidator;
  private final NGEncryptorService ngEncryptorService;

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
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("privateSecret") @DefaultValue("false") boolean privateSecret,
      @Valid @NotNull SecretRequestWrapper dto) {
    if (!Objects.equals(orgIdentifier, dto.getSecret().getOrgIdentifier())
        || !Objects.equals(projectIdentifier, dto.getSecret().getProjectIdentifier())) {
      throw new InvalidRequestException("Invalid request, scope in payload and params do not match.", USER);
    }

    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(SECRET_RESOURCE_TYPE, null),
        SECRET_EDIT_PERMISSION, privateSecret ? SecurityContextBuilder.getPrincipal() : null);
    if (privateSecret) {
      dto.getSecret().setOwner(SecurityContextBuilder.getPrincipal());
    }

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
    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);

    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_ACCESS_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);

    return ResponseDTO.newResponse(
        ngSecretService.validateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, metadata));
  }

  @POST
  @Path("/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Create a secret via yaml", nickname = "postSecretViaYaml")
  public ResponseDTO<SecretResponseWrapper> createViaYaml(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("privateSecret") @DefaultValue("false") boolean privateSecret, @Valid SecretRequestWrapper dto) {
    if (!Objects.equals(orgIdentifier, dto.getSecret().getOrgIdentifier())
        || !Objects.equals(projectIdentifier, dto.getSecret().getProjectIdentifier())) {
      throw new InvalidRequestException("Invalid request, scope in payload and params do not match.", USER);
    }
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(SECRET_RESOURCE_TYPE, null),
        SECRET_EDIT_PERMISSION, privateSecret ? SecurityContextBuilder.getPrincipal() : null);
    if (privateSecret) {
      dto.getSecret().setOwner(SecurityContextBuilder.getPrincipal());
    }

    return ResponseDTO.newResponse(ngSecretService.createViaYaml(accountIdentifier, dto.getSecret()));
  }

  @GET
  @ApiOperation(value = "Get secrets", nickname = "listSecretsV2")
  @Deprecated
  public ResponseDTO<PageResponse<SecretResponseWrapper>> list(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers,
      @QueryParam("type") SecretType secretType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("types") List<SecretType> secretTypes,
      @QueryParam("source_category") ConnectorCategory sourceCategory,
      @QueryParam(INCLUDE_SECRETS_FROM_EVERY_SUB_SCOPE) @DefaultValue("false") boolean includeSecretsFromEverySubScope,
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size) {
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(SECRET_RESOURCE_TYPE, null),
        SECRET_VIEW_PERMISSION, null);

    if (secretType != null) {
      secretTypes.add(secretType);
    }
    return ResponseDTO.newResponse(ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier,
        identifiers, secretTypes, includeSecretsFromEverySubScope, searchTerm, page, size, sourceCategory));
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "List secrets", nickname = "listSecretsV3")
  public ResponseDTO<PageResponse<SecretResponseWrapper>> listSecrets(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body SecretResourceFilterDTO secretResourceFilterDTO,
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size) {
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(SECRET_RESOURCE_TYPE, null),
        SECRET_VIEW_PERMISSION, null);
    return ResponseDTO.newResponse(ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier,
        secretResourceFilterDTO.getIdentifiers(), secretResourceFilterDTO.getSecretTypes(),
        secretResourceFilterDTO.isIncludeSecretsFromEverySubScope(), secretResourceFilterDTO.getSearchTerm(), page,
        size, secretResourceFilterDTO.getSourceCategory()));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecretV2")
  public ResponseDTO<SecretResponseWrapper> get(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_VIEW_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);

    return ResponseDTO.newResponse(secret);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete secret", nickname = "deleteSecretV2")
  public ResponseDTO<Boolean> delete(@PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_DELETE_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);
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
    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_EDIT_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);

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
    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_EDIT_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);

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

    SecretResponseWrapper secret =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null);
    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(SECRET_RESOURCE_TYPE, identifier), SECRET_EDIT_PERMISSION,
        secret != null ? secret.getSecret().getOwner() : null);

    return ResponseDTO.newResponse(ngSecretService.updateFile(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto.getSecret(), uploadedInputStream));
  }

  @POST
  @Path("files")
  @ApiOperation(value = "Create a secret file", nickname = "postSecretFileV2")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<SecretResponseWrapper> createSecretFile(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("privateSecret") @DefaultValue("false") boolean privateSecret,
      @NotNull @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("spec") String spec) {
    SecretRequestWrapper dto = JsonUtils.asObject(spec, SecretRequestWrapper.class);
    validateRequestPayload(dto);

    if (!Objects.equals(orgIdentifier, dto.getSecret().getOrgIdentifier())
        || !Objects.equals(projectIdentifier, dto.getSecret().getProjectIdentifier())) {
      throw new InvalidRequestException("Invalid request, scope in payload and params do not match.", USER);
    }

    secretPermissionValidator.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(SECRET_RESOURCE_TYPE, null),
        SECRET_EDIT_PERMISSION, privateSecret ? SecurityContextBuilder.getPrincipal() : null);
    if (privateSecret) {
      dto.getSecret().setOwner(SecurityContextBuilder.getPrincipal());
    }

    return ResponseDTO.newResponse(ngSecretService.createFile(accountIdentifier, dto.getSecret(), uploadedInputStream));
  }

  @POST
  @Path("decrypt-encryption-details")
  @Consumes("application/x-kryo")
  @Produces("application/x-kryo")
  @ApiOperation(hidden = true, value = "Decrypt Encrypted Details", nickname = "postDecryptEncryptedDetails")
  @InternalApi
  public ResponseDTO<DecryptableEntity> decryptEncryptedDetails(
      @Body DecryptableEntityWithEncryptionConsumers decryptableEntityWithEncryptionConsumers,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return ResponseDTO.newResponse(
        ngEncryptorService.decryptEncryptedDetails(decryptableEntityWithEncryptionConsumers.getDecryptableEntity(),
            decryptableEntityWithEncryptionConsumers.getEncryptedDataDetailList(), accountIdentifier));
  }

  @POST
  @Path("encryption-details")
  @Consumes("application/x-kryo")
  @Produces("application/x-kryo")
  @ApiOperation(hidden = true, value = "Get Encryption Details", nickname = "postEncryptionDetails")
  @InternalApi
  public ResponseDTO<List<EncryptedDataDetail>> getEncryptionDetails(
      @NotNull NGAccessWithEncryptionConsumer ngAccessWithEncryptionConsumer) {
    NGAccess ngAccess = ngAccessWithEncryptionConsumer.getNgAccess();
    DecryptableEntity decryptableEntity = ngAccessWithEncryptionConsumer.getDecryptableEntity();
    if (ngAccess == null || decryptableEntity == null) {
      return ResponseDTO.newResponse(new ArrayList<>());
    }
    for (Field field : decryptableEntity.getSecretReferenceFields()) {
      try {
        field.setAccessible(true);
        SecretRefData secretRefData = (SecretRefData) field.get(decryptableEntity);
        if (!Optional.ofNullable(secretRefData).isPresent() || secretRefData.isNull()) {
          continue;
        }
        Scope secretScope = secretRefData.getScope();
        SecretResponseWrapper secret =
            ngSecretService
                .get(ngAccess.getAccountIdentifier(), getOrgIdentifier(ngAccess.getOrgIdentifier(), secretScope),
                    getProjectIdentifier(ngAccess.getProjectIdentifier(), secretScope), secretRefData.getIdentifier())
                .orElse(null);
        secretPermissionValidator.checkForAccessOrThrow(
            ResourceScope.of(ngAccess.getAccountIdentifier(),
                getOrgIdentifier(ngAccess.getOrgIdentifier(), secretScope),
                getProjectIdentifier(ngAccess.getProjectIdentifier(), secretScope)),
            Resource.of(SECRET_RESOURCE_TYPE, secretRefData.getIdentifier()), SECRET_ACCESS_PERMISSION,
            secret != null ? secret.getSecret().getOwner() : null);

      } catch (IllegalAccessException illegalAccessException) {
        log.error("Error while checking access permission for secret: {}", field, illegalAccessException);
      }
    }
    return ResponseDTO.newResponse(encryptedDataService.getEncryptionDetails(
        ngAccessWithEncryptionConsumer.getNgAccess(), ngAccessWithEncryptionConsumer.getDecryptableEntity()));
  }

  private String getOrgIdentifier(String parentOrgIdentifier, @NotNull Scope scope) {
    if (scope != Scope.ACCOUNT) {
      return parentOrgIdentifier;
    }
    return null;
  }

  private String getProjectIdentifier(String parentProjectIdentifier, @NotNull Scope scope) {
    if (scope == Scope.PROJECT) {
      return parentProjectIdentifier;
    }
    return null;
  }
}
