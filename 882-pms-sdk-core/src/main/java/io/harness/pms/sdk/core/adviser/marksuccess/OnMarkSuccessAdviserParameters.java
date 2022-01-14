package io.harness.pms.sdk.core.adviser.marksuccess;

import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.adviser.WithFailureTypes;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnMarkSuccessAdviserParameters implements WithFailureTypes {
  String nextNodeId;
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
