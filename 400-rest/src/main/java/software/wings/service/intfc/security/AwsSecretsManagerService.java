/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AwsSecretsManagerConfig;

@OwnedBy(PL)
public interface AwsSecretsManagerService {
  AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId);

  String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  boolean deleteAwsSecretsManagerConfig(String accountId, String configId);

  void validateSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  void decryptAsmConfigSecrets(String accountId, AwsSecretsManagerConfig secretsManagerConfig, boolean maskSecret);
}
