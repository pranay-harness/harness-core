/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.security.encryption.EncryptedRecord;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class UpsertSecretTaskResponse implements DelegateTaskNotifyResponseData {
  private final EncryptedRecord encryptedRecord;
  @Setter private DelegateMetaInfo delegateMetaInfo;
}
