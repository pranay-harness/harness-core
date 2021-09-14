/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.KmsConfig;

@OwnedBy(PL)
@TargetModule(HarnessModule._360_CG_MANAGER)
public interface KmsService {
  KmsConfig getKmsConfig(String accountId, String entityId);

  String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig);

  KmsConfig getGlobalKmsConfig();

  String saveKmsConfig(String accountId, KmsConfig kmsConfig);

  boolean deleteKmsConfig(String accountId, String kmsConfigId);

  void decryptKmsConfigSecrets(String accountId, KmsConfig kmsConfig, boolean maskSecret);

  void validateSecretsManagerConfig(String accountId, KmsConfig kmsConfig);
}
