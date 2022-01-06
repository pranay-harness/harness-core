/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import software.wings.beans.GcpSecretsManagerConfig;

import java.util.List;

public interface GcpSecretsManagerServiceV2 {
  GcpSecretsManagerConfig getGcpSecretsManagerConfig(String accountId, String configId);

  String saveGcpSecretsManagerConfig(
      String accountId, GcpSecretsManagerConfig gcpSecretsManagerConfig, boolean validate);

  String updateGcpSecretsManagerConfig(String accountId, GcpSecretsManagerConfig gcpSecretsManagerConfig);

  boolean deleteGcpSecretsManagerConfig(String accountId, String configId);

  void validateSecretsManagerConfig(String accountId, GcpSecretsManagerConfig GcpSecretsManagerConfig);

  void decryptGcpConfigSecrets(GcpSecretsManagerConfig GcpSecretsManagerConfig, boolean maskSecret);

  List<String> getAllAvailableRegions(String accountId, String configId);
}
