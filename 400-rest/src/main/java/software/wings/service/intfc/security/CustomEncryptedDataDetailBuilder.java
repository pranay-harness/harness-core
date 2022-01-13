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
import io.harness.beans.EncryptedData;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

@OwnedBy(PL)
@TargetModule(HarnessModule._890_SM_CORE)
public interface CustomEncryptedDataDetailBuilder {
  EncryptedDataDetail buildEncryptedDataDetail(
      EncryptedData encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig);
}
