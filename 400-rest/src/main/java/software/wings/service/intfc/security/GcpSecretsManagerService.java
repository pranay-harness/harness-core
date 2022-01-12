/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.GcpKmsConfig;

@OwnedBy(PL)
public interface GcpSecretsManagerService {
  GcpKmsConfig getGcpKmsConfig(String accountId, String configId);

  String saveGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig, boolean validate);

  String updateGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig, boolean validate);

  String updateGcpKmsConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  GcpKmsConfig getGlobalKmsConfig();

  boolean deleteGcpKmsConfig(String accountId, String configId);

  void validateSecretsManagerConfig(String accountId, GcpKmsConfig gcpKmsConfig);

  void decryptGcpConfigSecrets(GcpKmsConfig gcpKmsConfig, boolean maskSecret);
}
