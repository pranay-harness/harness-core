package io.harness.ng.core.api.impl;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.fromDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.resources.secretsmanagement.EncryptedDataMapper;
import software.wings.security.encryption.EncryptedData;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretServiceImpl implements NGSecretService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedData get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return fromDTO(
        getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier)));
  }

  @Override
  public List<EncryptedData> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SecretType secretType, String searchTerm) {
    List<EncryptedDataDTO> encryptedDataDTOs = getResponse(
        secretManagerClient.listSecrets(accountIdentifier, orgIdentifier, projectIdentifier, secretType, searchTerm));
    return encryptedDataDTOs.stream().map(EncryptedDataMapper::fromDTO).collect(Collectors.toList());
  }

  @Override
  public EncryptedData create(SecretTextCreateDTO dto) {
    return fromDTO(getResponse(secretManagerClient.createSecret(dto)));
  }

  @Override
  public boolean update(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      SecretTextUpdateDTO dto) {
    return getResponse(
        secretManagerClient.updateSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier, dto));
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getResponse(
        secretManagerClient.deleteSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
