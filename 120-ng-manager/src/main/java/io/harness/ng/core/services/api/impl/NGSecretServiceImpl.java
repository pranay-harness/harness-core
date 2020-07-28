package io.harness.ng.core.services.api.impl;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.services.api.NGSecretService;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretServiceImpl implements NGSecretService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public EncryptedDataDTO getSecret(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getResponse(secretManagerClient.getSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }

  @Override
  public List<EncryptedDataDTO> listSecrets(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, SecretType secretType) {
    return getResponse(
        secretManagerClient.listSecrets(accountIdentifier, orgIdentifier, projectIdentifier, secretType));
  }

  @Override
  public EncryptedDataDTO createSecret(SecretTextCreateDTO dto) {
    return getResponse(secretManagerClient.createSecret(dto));
  }

  @Override
  public boolean updateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretTextUpdateDTO dto) {
    return getResponse(
        secretManagerClient.updateSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier, dto));
  }

  @Override
  public boolean deleteSecret(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getResponse(
        secretManagerClient.deleteSecret(identifier, accountIdentifier, orgIdentifier, projectIdentifier));
  }
}
