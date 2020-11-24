package io.harness.ng.core.api.impl;

import static java.util.Collections.emptyList;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.TGTKeyTabFilePathSpecDTO;
import io.harness.ng.core.dto.secrets.TGTPasswordSpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.Secret.SecretKeys;
import io.harness.ng.core.remote.SSHKeyValidationMetadata;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.repositories.ng.core.spring.SecretRepository;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NGSecretServiceV2Impl implements NGSecretServiceV2 {
  private final SecretRepository secretRepository;
  private final SecretManagerClientService secretManagerClientService;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;

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
      secretRepository.delete(secretV2Optional.get());
      return true;
    }
    return false;
  }

  @Override
  public Secret create(String accountIdentifier, SecretDTOV2 dto, boolean draft) {
    Secret secret = dto.toEntity();
    secret.setDraft(draft);
    secret.setAccountIdentifier(accountIdentifier);
    return secretRepository.save(secret);
  }

  @Override
  public boolean update(String accountIdentifier, SecretDTOV2 dto, boolean draft) {
    Optional<Secret> secretOptional =
        get(accountIdentifier, dto.getOrgIdentifier(), dto.getProjectIdentifier(), dto.getIdentifier());
    if (secretOptional.isPresent()) {
      Secret oldSecret = secretOptional.get();
      Secret newSecret = dto.toEntity();
      oldSecret.setDescription(newSecret.getDescription());
      oldSecret.setName(newSecret.getName());
      oldSecret.setTags(newSecret.getTags());
      oldSecret.setSecretSpec(newSecret.getSecretSpec());
      oldSecret.setDraft(oldSecret.isDraft() && draft);
      oldSecret.setType(newSecret.getType());
      secretRepository.save(oldSecret);
      return true;
    }
    return false;
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
      List<EncryptedDataDetail> encryptionDetails = getSSHKeyEncryptionDetails(secretSpecDTO, baseNGAccess);
      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(accountIdentifier)
                                                    .taskType("NG_SSH_VALIDATION")
                                                    .taskParameters(SSHTaskParams.builder()
                                                                        .host(sshKeyValidationMetadata.getHost())
                                                                        .encryptionDetails(encryptionDetails)
                                                                        .sshKeySpec(secretSpecDTO)
                                                                        .build())
                                                    .executionTimeout(Duration.ofMinutes(2L))
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

  private List<EncryptedDataDetail> getSSHKeyEncryptionDetails(SSHKeySpecDTO secretSpecDTO, BaseNGAccess baseNGAccess) {
    switch (secretSpecDTO.getAuth().getAuthScheme()) {
      case SSH:
        SSHConfigDTO sshConfigDTO = (SSHConfigDTO) secretSpecDTO.getAuth().getSpec();
        return getSSHEncryptionDetails(sshConfigDTO, baseNGAccess);
      case Kerberos:
        KerberosConfigDTO kerberosConfigDTO = (KerberosConfigDTO) secretSpecDTO.getAuth().getSpec();
        return getKerberosEncryptionDetails(kerberosConfigDTO, baseNGAccess);
      default:
        return emptyList();
    }
  }

  List<EncryptedDataDetail> getSSHEncryptionDetails(SSHConfigDTO sshConfigDTO, BaseNGAccess baseNGAccess) {
    switch (sshConfigDTO.getCredentialType()) {
      case Password:
        SSHPasswordCredentialDTO sshPasswordCredentialDTO = (SSHPasswordCredentialDTO) sshConfigDTO.getSpec();
        return secretManagerClientService.getEncryptionDetails(baseNGAccess, sshPasswordCredentialDTO);
      case KeyReference:
        SSHKeyReferenceCredentialDTO sshKeyReferenceCredentialDTO =
            (SSHKeyReferenceCredentialDTO) sshConfigDTO.getSpec();
        return secretManagerClientService.getEncryptionDetails(baseNGAccess, sshKeyReferenceCredentialDTO);
      case KeyPath:
        SSHKeyPathCredentialDTO sshKeyPathCredentialDTO = (SSHKeyPathCredentialDTO) sshConfigDTO.getSpec();
        return secretManagerClientService.getEncryptionDetails(baseNGAccess, sshKeyPathCredentialDTO);
      default:
        return emptyList();
    }
  }

  private List<EncryptedDataDetail> getKerberosEncryptionDetails(
      KerberosConfigDTO kerberosConfigDTO, BaseNGAccess baseNGAccess) {
    switch (kerberosConfigDTO.getTgtGenerationMethod()) {
      case Password:
        TGTPasswordSpecDTO tgtPasswordSpecDTO = (TGTPasswordSpecDTO) kerberosConfigDTO.getSpec();
        return secretManagerClientService.getEncryptionDetails(baseNGAccess, tgtPasswordSpecDTO);
      case KeyTabFilePath:
        TGTKeyTabFilePathSpecDTO tgtKeyTabFilePathSpecDTO = (TGTKeyTabFilePathSpecDTO) kerberosConfigDTO.getSpec();
        return secretManagerClientService.getEncryptionDetails(baseNGAccess, tgtKeyTabFilePathSpecDTO);
      default:
        return emptyList();
    }
  }

  @Override
  public Page<Secret> list(Criteria criteria, int page, int size) {
    return secretRepository.findAll(
        criteria, PageUtils.getPageRequest(page, size, Collections.singletonList(SecretKeys.createdAt + ",desc")));
  }
}
