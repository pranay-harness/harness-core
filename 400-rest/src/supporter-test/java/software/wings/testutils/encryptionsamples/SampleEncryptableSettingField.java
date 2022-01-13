/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.testutils.encryptionsamples;

import io.harness.beans.EncryptedData;
import io.harness.persistence.UuidAware;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleEncryptableSettingField {
  private String accountId;
  private EncryptedData random;
  private UuidAware entity;
}
