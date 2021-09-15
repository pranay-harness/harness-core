/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
