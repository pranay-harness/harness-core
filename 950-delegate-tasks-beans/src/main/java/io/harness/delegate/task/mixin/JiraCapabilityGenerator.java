package io.harness.delegate.task.mixin;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class JiraCapabilityGenerator {
  public static List<ExecutionCapability> generateDelegateCapabilities(ExecutionCapabilityDemander capabilityDemander,
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (capabilityDemander != null) {
      executionCapabilities.addAll(capabilityDemander.fetchRequiredExecutionCapabilities(maskingEvaluator));
    }
    if (isEmpty(encryptedDataDetails)) {
      return executionCapabilities;
    }

    executionCapabilities.addAll(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
        encryptedDataDetails, maskingEvaluator));
    return executionCapabilities;
  }
}
