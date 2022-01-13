/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
