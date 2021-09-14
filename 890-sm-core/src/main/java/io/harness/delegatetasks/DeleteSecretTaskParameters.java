/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.delegatetasks;

import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteSecretTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private final EncryptedRecord existingRecord;
  private final EncryptionConfig encryptionConfig;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return ((SecretManagerConfig) encryptionConfig).fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
