package io.harness.ng.core.api.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.SecretManagementModule.SECRET_FILE_SERVICE;
import static io.harness.ng.core.SecretManagementModule.SECRET_TEXT_SERVICE;
import static io.harness.ng.core.SecretManagementModule.SSH_SECRET_SERVICE;
import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
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
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class SecretCrudServiceImpl implements SecretCrudService {
  private final SecretManagerClient secretManagerClient;
  private final NGSecretServiceV2 ngSecretService;
  private final FileUploadLimit fileUploadLimit;
  private final SecretEntityReferenceHelper secretEntityReferenceHelper;
  private final Map<SecretType, SecretModifyService> secretTypeToServiceMap;

  @Inject
  public SecretCrudServiceImpl(SecretManagerClient secretManagerClient,
      @Named(SECRET_TEXT_SERVICE) SecretModifyService secretTextService,
      @Named(SECRET_FILE_SERVICE) SecretModifyService secretFileService,
      @Named(SSH_SECRET_SERVICE) SecretModifyService sshSecretService,
      SecretEntityReferenceHelper secretEntityReferenceHelper, FileUploadLimit fileUploadLimit,
      NGSecretServiceV2 ngSecretService) {
    this.secretManagerClient = secretManagerClient;
    this.fileUploadLimit = fileUploadLimit;
    this.secretEntityReferenceHelper = secretEntityReferenceHelper;
    this.ngSecretService = ngSecretService;
    secretTypeToServiceMap = new EnumMap<>(ImmutableMap.of(SecretType.SecretText, secretTextService,
        SecretType.SecretFile, secretFileService, SecretType.SSHKey, sshSecretService));
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
  public SecretResponseWrapper create(String accountIdentifier, SecretDTOV2 dto) {
    EncryptedDataDTO encryptedData = getService(dto.getType()).create(accountIdentifier, dto);
    if (Optional.ofNullable(encryptedData).isPresent()) {
      secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedData);
      Secret secret = ngSecretService.create(accountIdentifier, dto, false);
      return getResponseWrapper(secret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to create secret remotely.", USER);
  }

  @Override
  public SecretResponseWrapper createViaYaml(@NotNull String accountIdentifier, SecretDTOV2 dto) {
    if (!dto.getSpec().isValidYaml()) {
      throw new InvalidRequestException("Yaml not valid.", USER);
    }
    EncryptedDataDTO encryptedData = getService(dto.getType()).create(accountIdentifier, dto);
    if (Optional.ofNullable(encryptedData).isPresent()) {
      secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedData);
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
    if (secretV2Optional.isPresent()) {
      return Optional.ofNullable(getResponseWrapper(secretV2Optional.get()));
    }
    return Optional.empty();
  }

  @Override
  public PageResponse<SecretResponseWrapper> list(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, SecretType secretType, String searchTerm, int page, int size) {
    Criteria criteria = Criteria.where(SecretKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(SecretKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(SecretKeys.projectIdentifier)
                            .is(projectIdentifier);
    if (secretType != null) {
      criteria.and(SecretKeys.type).is(secretType);
    }
    if (!StringUtils.isEmpty(searchTerm)) {
      // TODO{phoenikx-secret} Add search for tags here using or operator
      criteria.and(SecretKeys.name).regex(searchTerm, "i");
    }
    Page<Secret> secrets = ngSecretService.list(criteria, page, size);
    return PageUtils.getNGPageResponse(
        secrets, secrets.getContent().stream().map(this::getResponseWrapper).collect(Collectors.toList()));
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    EncryptedDataDTO encryptedData =
        getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));

    boolean remoteDeletionSuccess =
        getResponse(secretManagerClient.deleteSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
    boolean localDeletionSuccess = false;
    if (remoteDeletionSuccess) {
      localDeletionSuccess = ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    }
    if (remoteDeletionSuccess && localDeletionSuccess) {
      secretEntityReferenceHelper.deleteSecretEntityReferenceWhenSecretGetsDeleted(encryptedData);
      return true;
    }
    if (!remoteDeletionSuccess) {
      throw new InvalidRequestException("Unable to delete secret remotely.", USER);
    } else {
      throw new InvalidRequestException("Unable to delete secret locally, data might be inconsistent", USER);
    }
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto) {
    boolean remoteUpdateSuccess = getService(dto.getType()).update(accountIdentifier, dto);
    boolean localUpdateSuccess = false;
    if (remoteUpdateSuccess) {
      localUpdateSuccess = ngSecretService.update(accountIdentifier, dto, false);
    }
    if (remoteUpdateSuccess && localUpdateSuccess) {
      return true;
    }
    if (!remoteUpdateSuccess) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to update secret remotely", USER);
    } else {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Unable to update secret locally, data might be inconsistent", USER);
    }
  }

  @Override
  public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 dto) {
    if (!dto.getSpec().isValidYaml()) {
      throw new InvalidRequestException("Invalid Yaml", USER);
    }
    boolean remoteUpdateSuccess = getService(dto.getType()).update(accountIdentifier, dto);
    boolean localUpdateSuccess = false;
    if (remoteUpdateSuccess) {
      localUpdateSuccess = ngSecretService.update(accountIdentifier, dto, true);
    }
    if (remoteUpdateSuccess && localUpdateSuccess) {
      return true;
    }
    if (!remoteUpdateSuccess) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to update secret remotely", USER);
    } else {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Unable to update secret locally, data might be inconsistent", USER);
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
      secretEntityReferenceHelper.createEntityReferenceForSecret(encryptedData);
      Secret secret = ngSecretService.create(accountIdentifier, dto, false);
      return getResponseWrapper(secret);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to create secret file remotely", USER);
  }

  @SneakyThrows
  @Override
  public boolean updateFile(String accountIdentifier, SecretDTOV2 dto, @NotNull InputStream inputStream) {
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
      return ngSecretService.update(accountIdentifier, dto, false);
    }
    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to update secret file remotely", USER);
  }

  @Override
  public SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, SecretValidationMetaData metadata) {
    return ngSecretService.validateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, metadata);
  }
}
