/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.CyberArkConfig;

@OwnedBy(PL)
public interface CyberArkService {
  CyberArkConfig getConfig(String accountId, String configId);

  String saveConfig(String accountId, CyberArkConfig cyberArkConfig);

  boolean deleteConfig(String accountId, String configId);

  void validateConfig(CyberArkConfig secretsManagerConfig);

  void decryptCyberArkConfigSecrets(String accountId, CyberArkConfig cyberArkConfig, boolean maskSecret);
}
