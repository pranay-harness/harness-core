package io.harness.ng.core.services.api.impl;

import static io.harness.ng.core.utils.SecretUtils.getResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.ng.core.services.api.NGSecretManagerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SecretManagerConfig;

import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class NGSecretManagerServiceImpl implements NGSecretManagerService {
  private final SecretManagerClient secretManagerClient;

  @Override
  public List<SecretManagerConfig> listSecretManagers(String accountId) {
    return getResponse(secretManagerClient.getSecretManagersForAccount(accountId));
  }

  @Override
  public String saveOrUpdateSecretManager(String accountId, SecretManagerConfig secretManagerConfig) {
    return getResponse(secretManagerClient.createOrUpdateSecretManager(accountId, secretManagerConfig));
  }

  @Override
  public boolean deleteSecretManager(String accountId, String kmsId) {
    return getResponse(secretManagerClient.deleteSecretManager(kmsId, accountId));
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String kmsId) {
    return getResponse(secretManagerClient.getSecretManager(kmsId, accountId));
  }
}
