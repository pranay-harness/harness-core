package io.harness.pms.sdk.core.adviser.manualintervention;

import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.adviser.WithFailureTypes;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class ManualInterventionAdviserParameters implements WithFailureTypes {
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
  Integer timeout;
  RepairActionCode timeoutAction;

  // Config only used when timeoutAction is Retry.
  RetryAdviserParameters retryAdviserParameters;
  // Only applicable if the timeout action is set to IGNORE
  String nextNodeId;
}
