package io.harness.ng.core.services.api.impl;

import static io.harness.secretmanagerclient.utils.SecretManagerClientUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.encryption.SecretType;
import io.harness.ng.core.services.api.NGSecretService;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
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
  public EncryptedDataDTO getSecretById(String accountId, String id) {
    return getResponse(secretManagerClient.getSecretById(id, accountId, null));
  }

  @Override
  public String createSecret(String accountId, boolean localMode, SecretTextDTO secretText) {
    return getResponse(secretManagerClient.createSecret(accountId, localMode, secretText));
  }

  @Override
  public boolean updateSecret(String accountId, String uuId, SecretTextDTO secretText) {
    return getResponse(secretManagerClient.updateSecret(accountId, uuId, secretText));
  }

  @Override
  public boolean deleteSecret(String accountId, String uuId) {
    return getResponse(secretManagerClient.deleteSecret(accountId, uuId));
  }

  @Override
  public List<EncryptedDataDTO> getSecretsByType(String accountId, SecretType secretType) {
    return getResponse(secretManagerClient.getSecretsForAccountByType(accountId, secretType));
  }
}
