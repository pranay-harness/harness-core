/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.LocalEncryptionConfig;

/**
 * @author marklu on 2019-05-14
 */
@OwnedBy(PL)
public interface LocalSecretManagerService {
  LocalEncryptionConfig getEncryptionConfig(String accountId);

  String saveLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig);

  void validateLocalEncryptionConfig(String accountId, LocalEncryptionConfig localEncryptionConfig);

  boolean deleteLocalEncryptionConfig(String accountId, String uuid);
}
