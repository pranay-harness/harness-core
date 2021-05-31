package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.api.NGSecretActivityService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.events.SecretCreateEvent;
import io.harness.ng.core.events.SecretDeleteEvent;
import io.harness.ng.core.events.SecretUpdateEvent;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.ng.core.remote.SSHKeyValidationMetadata;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.repositories.ng.core.spring.SecretRepository;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Singleton
@Slf4j
public class NGSecretServiceV2Impl implements NGSecretServiceV2 {
  private final SecretRepository secretRepository;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final SshKeySpecDTOHelper sshKeySpecDTOHelper;
  private final NGSecretActivityService ngSecretActivityService;

  private final OutboxService outboxService;
  private final TransactionTemplate transactionTemplate;
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public NGSecretServiceV2Impl(SecretRepository secretRepository, DelegateGrpcClientWrapper delegateGrpcClientWrapper,
      SshKeySpecDTOHelper sshKeySpecDTOHelper, NGSecretActivityService ngSecretActivityService,
      OutboxService outboxService, @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.secretRepository = secretRepository;
    this.outboxService = outboxService;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
    this.sshKeySpecDTOHelper = sshKeySpecDTOHelper;
    this.ngSecretActivityService = ngSecretActivityService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return !secretRepository.existsByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public Optional<Secret> get(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    return secretRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public boolean delete(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier) {
    Optional<Secret> secretV2Optional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretV2Optional.isPresent()) {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        secretRepository.delete(secretV2Optional.get());
        deleteSecretActivities(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
        outboxService.save(new SecretDeleteEvent(accountIdentifier, secretV2Optional.get().toDTO()));
        return true;
      }));
    }
    return false;
  }

  private void deleteSecretActivities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    try {
      String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier, identifier);
      ngSecretActivityService.deleteAllActivities(accountIdentifier, fullyQualifiedIdentifier);
    } catch (Exception ex) {
      log.info("Error while deleting secret activity", ex);
    }
  }

  @Override
  public Secret create(String accountIdentifier, @Valid SecretDTOV2 secretDTO, boolean draft) {
    Secret secret = secretDTO.toEntity();
    secret.setDraft(draft);
    secret.setAccountIdentifier(accountIdentifier);
    try {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        Secret savedSecret = secretRepository.save(secret);
        createSecretCreationActivity(accountIdentifier, secretDTO);
        outboxService.save(new SecretCreateEvent(accountIdentifier, savedSecret.toDTO()));
        return savedSecret;
      }));
    } catch (DuplicateKeyException duplicateKeyException) {
      throw new DuplicateFieldException(
          "Duplicate identifier, please try again with a new identifier", USER, duplicateKeyException);
    }
  }

  private void createSecretCreationActivity(String accountIdentifier, SecretDTOV2 dto) {
    try {
      ngSecretActivityService.create(accountIdentifier, dto, NGActivityType.ENTITY_CREATION);
    } catch (Exception ex) {
      log.info("Error while creating secret creation activity", ex);
    }
  }

  @Override
  public Secret update(String accountIdentifier, @Valid SecretDTOV2 secretDTO, boolean draft) {
    Optional<Secret> secretOptional = get(
        accountIdentifier, secretDTO.getOrgIdentifier(), secretDTO.getProjectIdentifier(), secretDTO.getIdentifier());
    if (secretOptional.isPresent()) {
      Secret oldSecret = secretOptional.get();
      SecretDTOV2 oldSecretClone = (SecretDTOV2) NGObjectMapperHelper.clone(oldSecret.toDTO());

      Secret newSecret = secretDTO.toEntity();
      oldSecret.setDescription(newSecret.getDescription());
      oldSecret.setName(newSecret.getName());
      oldSecret.setTags(newSecret.getTags());
      oldSecret.setSecretSpec(newSecret.getSecretSpec());
      oldSecret.setDraft(oldSecret.isDraft() && draft);
      oldSecret.setType(newSecret.getType());
      try {
        return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
          secretRepository.save(oldSecret);
          outboxService.save(new SecretUpdateEvent(accountIdentifier, oldSecret.toDTO(), oldSecretClone));
          createSecretUpdateActivity(accountIdentifier, secretDTO);
          return oldSecret;
        }));
      } catch (DuplicateKeyException duplicateKeyException) {
        throw new DuplicateFieldException(
            "Duplicate identifier, please try again with a new identifier", USER, duplicateKeyException);
      }
    }
    throw new InvalidRequestException("No such secret found", USER_SRE);
  }

  private void createSecretUpdateActivity(String accountIdentifier, SecretDTOV2 dto) {
    try {
      ngSecretActivityService.create(accountIdentifier, dto, NGActivityType.ENTITY_UPDATE);
    } catch (Exception ex) {
      log.info("Error while creating secret update activity", ex);
    }
  }

  @Override
  public SecretValidationResultDTO validateSecret(@NotNull String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String identifier, @NotNull SecretValidationMetaData metadata) {
    Optional<Secret> secretV2Optional = get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretV2Optional.isPresent() && secretV2Optional.get().getType() == SecretType.SSHKey) {
      SSHKeyValidationMetadata sshKeyValidationMetadata = (SSHKeyValidationMetadata) metadata;
      SSHKeySpecDTO secretSpecDTO = (SSHKeySpecDTO) secretV2Optional.get().getSecretSpec().toDTO();
      BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                      .accountIdentifier(accountIdentifier)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
      List<EncryptedDataDetail> encryptionDetails =
          sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(secretSpecDTO, baseNGAccess);
      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(accountIdentifier)
                                                    .taskType("NG_SSH_VALIDATION")
                                                    .taskParameters(SSHTaskParams.builder()
                                                                        .host(sshKeyValidationMetadata.getHost())
                                                                        .encryptionDetails(encryptionDetails)
                                                                        .sshKeySpec(secretSpecDTO)
                                                                        .build())
                                                    .executionTimeout(Duration.ofSeconds(45))
                                                    .build();
      DelegateResponseData delegateResponseData = this.delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (delegateResponseData instanceof RemoteMethodReturnValueData) {
        return SecretValidationResultDTO.builder()
            .success(false)
            .message(((RemoteMethodReturnValueData) delegateResponseData).getException().getMessage())
            .build();
      } else if (delegateResponseData instanceof SSHConfigValidationTaskResponse) {
        SSHConfigValidationTaskResponse responseData = (SSHConfigValidationTaskResponse) delegateResponseData;
        return SecretValidationResultDTO.builder()
            .success(responseData.isConnectionSuccessful())
            .message(responseData.getErrorMessage())
            .build();
      }
    }
    return SecretValidationResultDTO.builder().success(false).message("Validation failed.").build();
  }

  @Override
  public Page<Secret> list(Criteria criteria, int page, int size) {
    return secretRepository.findAll(
        criteria, PageUtils.getPageRequest(page, size, Collections.singletonList(SecretKeys.createdAt + ",desc")));
  }
}
