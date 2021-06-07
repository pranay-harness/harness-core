package io.harness.ng.core.api.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.SecretManagementModule.SECRET_FILE_SERVICE;
import static io.harness.ng.core.SecretManagementModule.SECRET_TEXT_SERVICE;
import static io.harness.ng.core.SecretManagementModule.SSH_SECRET_SERVICE;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.secretmanagerclient.SecretType.SecretText;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.common.beans.NGTag.NGTagKeys;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import io.harness.serializer.JsonUtils;
import io.harness.stream.BoundedInputStream;
import io.harness.utils.PageUtils;

import software.wings.app.FileUploadLimit;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@Slf4j
public class SecretCrudServiceImpl implements SecretCrudService {
  private final SecretManagerClient secretManagerClient;
  private final NGSecretServiceV2 ngSecretService;
  private final FileUploadLimit fileUploadLimit;
  private final SecretEntityReferenceHelper secretEntityReferenceHelper;
  private final Producer eventProducer;
  private final Map<SecretType, SecretModifyService> secretTypeToServiceMap;

  @Inject
  public SecretCrudServiceImpl(SecretManagerClient secretManagerClient,
      @Named(SECRET_TEXT_SERVICE) SecretModifyService secretTextService,
      @Named(SECRET_FILE_SERVICE) SecretModifyService secretFileService,
      @Named(SSH_SECRET_SERVICE) SecretModifyService sshSecretService,
      SecretEntityReferenceHelper secretEntityReferenceHelper, FileUploadLimit fileUploadLimit,
      NGSecretServiceV2 ngSecretService, @Named(ENTITY_CRUD) Producer eventProducer) {
    this.secretManagerClient = secretManagerClient;
    this.fileUploadLimit = fileUploadLimit;
    this.secretEntityReferenceHelper = secretEntityReferenceHelper;
    this.ngSecretService = ngSecretService;
    this.eventProducer = eventProducer;
    secretTypeToServiceMap = new EnumMap<>(ImmutableMap.of(
        SecretText, secretTextService, SecretType.SecretFile, secretFileService, SecretType.SSHKey, sshSecretService));
  }

  private void checkEqualityOrThrow(Object str1, Object str2) {
    if (!Objects.equals(str1, str2)) {
      throw new InvalidRequestException(
          "Cannot change organization, project, identifier or type of a secret after creation.", INVALID_REQUEST, USER);
    }
  }

  private void validateUpdateRequest(
      String orgIdentifier, String projectIdentifier, String identifier, SecretType secretType, SecretDTOV2 updateDTO) {
    checkEqualityOrThrow(orgIdentifier, updateDTO.getOrgIdentifier());
    checkEqualityOrThrow(projectIdentifier, updateDTO.getProjectIdentifier());
    checkEqualityOrThrow(identifier, updateDTO.getIdentifier());
    checkEqualityOrThrow(secretType, updateDTO.getType());
  }

  private SecretModifyService getService(SecretType secretType) {
    SecretModifyService secretModifyService = secretTypeToServiceMap.get(secretType);
    if (secretModifyService == null) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "No handler found for secret type: " + secretType, SRE);
    }
    return secretModifyService;
  }

  private SecretResponseWrapper getResponseWrapper(@NotNull Secret secret) {
    return SecretResponseWrapper.builder()
        .secret(secret.toDTO())
        .updatedAt(secret.getLastModifiedAt())
        .createdAt(secret.getCreatedAt())
        .draft(secret.isDraft())
        .build();
  }

  @Override
  public Boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return ngSecretService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public SecretResponseWrapper create(String accountIdentifier, SecretDTOV2 dto) {
    if (SecretText.equals(dto.getType()) && isEmpty(((SecretTextSpecDTO) dto.getSpec()).getValue())) {
      throw new InvalidRequestException("value cannot be empty for a secret text.");
    }
    EncryptedDataDTO encryptedData = getService(dto.getType()).create(accountIdentifier, dto);
    if (Optional.ofNullable(encryptedData).isPresent()) {
      secretEntityReferenceHelper.createSetupUsageForSecretManager(encryptedData);
      Secret secret = ngSecretService.create(accountIdentifier, dto, false);
      return getResponseWrapper(secret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to create secret remotely.", USER);
  }

  @Override
  public SecretResponseWrapper createViaYaml(@NotNull String accountIdentifier, SecretDTOV2 dto) {
    Optional<String> message = dto.getSpec().getErrorMessageForInvalidYaml();
    if (message.isPresent()) {
      throw new InvalidRequestException(message.get(), USER);
    }

    EncryptedDataDTO encryptedData = getService(dto.getType()).create(accountIdentifier, dto);
    if (Optional.ofNullable(encryptedData).isPresent()) {
      secretEntityReferenceHelper.createSetupUsageForSecretManager(encryptedData);
      Secret secret = ngSecretService.create(accountIdentifier, dto, true);
      return getResponseWrapper(secret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to create secret remotely.", USER);
  }

  @Override
  public Optional<SecretResponseWrapper> get(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    Optional<Secret> secretV2Optional =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return secretV2Optional.map(this::getResponseWrapper);
  }

  @Override
  public PageResponse<SecretResponseWrapper> list(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<String> identifiers, List<SecretType> secretTypes,
      boolean includeSecretsFromEverySubScope, String searchTerm, int page, int size) {
    Criteria criteria = Criteria.where(SecretKeys.accountIdentifier).is(accountIdentifier);
    if (!includeSecretsFromEverySubScope) {
      criteria.and(SecretKeys.orgIdentifier).is(orgIdentifier).and(SecretKeys.projectIdentifier).is(projectIdentifier);
    } else {
      if (isNotBlank(orgIdentifier)) {
        criteria.and(SecretKeys.orgIdentifier).is(orgIdentifier);
        if (isNotBlank(projectIdentifier)) {
          criteria.and(SecretKeys.projectIdentifier).is(projectIdentifier);
        }
      }
    }
    if (isNotEmpty(secretTypes)) {
      criteria = criteria.and(SecretKeys.type).in(secretTypes);
    }
    if (!StringUtils.isEmpty(searchTerm)) {
      criteria = criteria.orOperator(
          Criteria.where(SecretKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          Criteria.where(SecretKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          Criteria.where(SecretKeys.tags + "." + NGTagKeys.key)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          Criteria.where(SecretKeys.tags + "." + NGTagKeys.value)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
    }
    if (Objects.nonNull(identifiers) && !identifiers.isEmpty()) {
      criteria.and(SecretKeys.identifier).in(identifiers);
    }
    Page<Secret> secrets = ngSecretService.list(criteria, page, size);
    return PageUtils.getNGPageResponse(
        secrets, secrets.getContent().stream().map(this::getResponseWrapper).collect(Collectors.toList()));
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    EncryptedDataDTO encryptedData =
        getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
    Optional<SecretResponseWrapper> optionalSecret =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);

    boolean remoteDeletionSuccess = true, localDeletionSuccess = false;
    if (encryptedData != null) {
      remoteDeletionSuccess = getResponse(
          secretManagerClient.deleteSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
    }

    if (remoteDeletionSuccess) {
      localDeletionSuccess = ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    }
    if (remoteDeletionSuccess && localDeletionSuccess) {
      if (encryptedData != null) {
        secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(encryptedData);
      } else {
        optionalSecret.ifPresent(secretResponseWrapper
            -> secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(
                EncryptedDataDTO.builder()
                    .account(accountIdentifier)
                    .org(orgIdentifier)
                    .project(projectIdentifier)
                    .identifier(identifier)
                    .secretManager(getSecretManagerIdentifier(secretResponseWrapper.getSecret()))
                    .build()));
      }
      publishEvent(accountIdentifier, orgIdentifier, projectIdentifier, identifier,
          EventsFrameworkMetadataConstants.DELETE_ACTION);
      return true;
    }
    if (!remoteDeletionSuccess) {
      throw new InvalidRequestException("Unable to delete secret remotely.", USER);
    } else {
      throw new InvalidRequestException("Unable to delete secret locally, data might be inconsistent", USER);
    }
  }

  private void publishEvent(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String action) {
    try {
      EntityChangeDTO.Builder secretEntityChangeDTOBuilder =
          EntityChangeDTO.newBuilder()
              .setAccountIdentifier(StringValue.of(accountIdentifier))
              .setIdentifier(StringValue.of(identifier));
      if (isNotBlank(orgIdentifier)) {
        secretEntityChangeDTOBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
      }
      if (isNotBlank(projectIdentifier)) {
        secretEntityChangeDTOBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
      }
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(
                  ImmutableMap.of("accountId", accountIdentifier, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                      EventsFrameworkMetadataConstants.SECRET_ENTITY, EventsFrameworkMetadataConstants.ACTION, action))
              .setData(secretEntityChangeDTOBuilder.build().toByteString())
              .build());
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework secret Identifier: {}", identifier, e);
    }
  }

  private String getSecretManagerIdentifier(SecretDTOV2 secret) {
    switch (secret.getType()) {
      case SecretText:
        return ((SecretTextSpecDTO) secret.getSpec()).getSecretManagerIdentifier();
      case SecretFile:
        return ((SecretFileSpecDTO) secret.getSpec()).getSecretManagerIdentifier();
      default:
        return HARNESS_SECRET_MANAGER_IDENTIFIER;
    }
  }

  private SecretResponseWrapper processAndGetSecret(boolean remoteUpdateSuccess, Secret updatedSecret) {
    if (remoteUpdateSuccess && updatedSecret != null) {
      publishEvent(updatedSecret, EventsFrameworkMetadataConstants.UPDATE_ACTION);
      return getResponseWrapper(updatedSecret);
    }
    if (!remoteUpdateSuccess) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to update secret remotely", USER);
    } else {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Unable to update secret locally, data might be inconsistent", USER);
    }
  }

  @Override
  public SecretResponseWrapper update(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, SecretDTOV2 dto) {
    SecretDTOV2 existingSecret =
        validateUpdateRequestAndGetSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto);

    boolean remoteUpdateSuccess = getService(dto.getType()).update(accountIdentifier, existingSecret, dto);
    Secret updatedSecret = null;
    if (remoteUpdateSuccess) {
      updatedSecret = ngSecretService.update(accountIdentifier, dto, false);
    }
    return processAndGetSecret(remoteUpdateSuccess, updatedSecret);
  }

  @Override
  public SecretResponseWrapper updateViaYaml(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, SecretDTOV2 dto) {
    if (dto.getSpec().getErrorMessageForInvalidYaml().isPresent()) {
      throw new InvalidRequestException(dto.getSpec().getErrorMessageForInvalidYaml().get(), USER);
    }

    SecretDTOV2 existingSecret =
        validateUpdateRequestAndGetSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto);

    boolean remoteUpdateSuccess = getService(dto.getType()).update(accountIdentifier, existingSecret, dto);
    Secret updatedSecret = null;
    if (remoteUpdateSuccess) {
      updatedSecret = ngSecretService.update(accountIdentifier, dto, true);
    }
    return processAndGetSecret(remoteUpdateSuccess, updatedSecret);
  }

  private void publishEvent(Secret secret, String action) {
    try {
      EntityChangeDTO.Builder secretEntityChangeDTOBuilder =
          EntityChangeDTO.newBuilder()
              .setAccountIdentifier(StringValue.of(secret.getAccountIdentifier()))
              .setIdentifier(StringValue.of(secret.getIdentifier()));
      if (isNotBlank(secret.getOrgIdentifier())) {
        secretEntityChangeDTOBuilder.setOrgIdentifier(StringValue.of(secret.getOrgIdentifier()));
      }
      if (isNotBlank(secret.getProjectIdentifier())) {
        secretEntityChangeDTOBuilder.setProjectIdentifier(StringValue.of(secret.getProjectIdentifier()));
      }
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", secret.getAccountIdentifier(),
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.SECRET_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, action))
              .setData(secretEntityChangeDTOBuilder.build().toByteString())
              .build());
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework secret Identifier: " + secret.getIdentifier(), e);
    }
  }

  @SneakyThrows
  @Override
  public SecretResponseWrapper createFile(
      @NotNull String accountIdentifier, @NotNull SecretDTOV2 dto, @NotNull InputStream inputStream) {
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    SecretFileDTO secretFileDTO = SecretFileDTO.builder()
                                      .account(accountIdentifier)
                                      .org(dto.getOrgIdentifier())
                                      .project(dto.getProjectIdentifier())
                                      .identifier(dto.getIdentifier())
                                      .name(dto.getName())
                                      .description(dto.getDescription())
                                      .tags(null)
                                      .secretManager(specDTO.getSecretManagerIdentifier())
                                      .type(dto.getType())
                                      .build();
    EncryptedDataDTO encryptedData =
        getResponse(secretManagerClient.createSecretFile(getRequestBody(JsonUtils.asJson(secretFileDTO)),
            getRequestBody(ByteStreams.toByteArray(
                new BoundedInputStream(inputStream, fileUploadLimit.getEncryptedFileLimit())))));
    if (Optional.ofNullable(encryptedData).isPresent()) {
      secretEntityReferenceHelper.createSetupUsageForSecretManager(encryptedData);
      Secret secret = ngSecretService.create(accountIdentifier, dto, false);
      return getResponseWrapper(secret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to create secret file remotely", USER);
  }

  private SecretDTOV2 validateUpdateRequestAndGetSecret(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, SecretDTOV2 updateDTO) {
    Optional<SecretResponseWrapper> secretOptional =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (!secretOptional.isPresent()) {
      throw new InvalidRequestException("No such secret found, please check identifier/scope and try again.");
    }

    SecretDTOV2 existingSecret = secretOptional.get().getSecret();
    validateUpdateRequest(existingSecret.getOrgIdentifier(), existingSecret.getProjectIdentifier(),
        existingSecret.getIdentifier(), existingSecret.getType(), updateDTO);
    return existingSecret;
  }

  @SneakyThrows
  @Override
  public SecretResponseWrapper updateFile(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, @Valid SecretDTOV2 dto, @NotNull InputStream inputStream) {
    validateUpdateRequestAndGetSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto);

    EncryptedDataDTO encryptedDataDTO = getResponse(secretManagerClient.getSecret(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier()));
    SecretFileSpecDTO specDTO = (SecretFileSpecDTO) dto.getSpec();
    if (encryptedDataDTO == null || !specDTO.getSecretManagerIdentifier().equals(encryptedDataDTO.getSecretManager())) {
      throw new InvalidRequestException("Cannot update secret manager after creating secret.", USER);
    }
    SecretFileUpdateDTO secretFileUpdateDTO =
        SecretFileUpdateDTO.builder().name(dto.getName()).tags(null).description(dto.getDescription()).build();
    RequestBody file = null;
    if (inputStream != null) {
      file = getRequestBody(
          ByteStreams.toByteArray(new BoundedInputStream(inputStream, fileUploadLimit.getEncryptedFileLimit())));
    }
    RequestBody metadata = getRequestBody(JsonUtils.asJson(secretFileUpdateDTO));
    boolean success = getResponse(secretManagerClient.updateSecretFile(
        dto.getIdentifier(), accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), file, metadata));
    if (success) {
      Secret updatedSecret = ngSecretService.update(accountIdentifier, dto, false);
      publishEvent(updatedSecret, EventsFrameworkMetadataConstants.UPDATE_ACTION);
      return getResponseWrapper(updatedSecret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to update secret file remotely", USER);
  }

  @Override
  public SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, @Valid SecretValidationMetaData metadata) {
    return ngSecretService.validateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, metadata);
  }
}
