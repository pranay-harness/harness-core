package io.harness.ng.core.services.api;

import software.wings.beans.SecretManagerConfig;

import java.util.List;

public interface NGSecretManagerService {
  String saveOrUpdateSecretManager(String accountId, SecretManagerConfig secretManagerConfig);

  boolean deleteSecretManager(String accountId, String kmsId);

  List<SecretManagerConfig> listSecretManagers(String accountId);

  SecretManagerConfig getSecretManager(String accountId, String kmsId);
}
