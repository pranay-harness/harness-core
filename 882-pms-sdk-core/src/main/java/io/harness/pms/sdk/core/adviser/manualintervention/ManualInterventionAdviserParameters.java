package io.harness.pms.sdk.core.adviser.manualintervention;

import io.harness.pms.execution.failure.FailureType;
import io.harness.pms.sdk.core.adviser.WithFailureTypes;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualInterventionAdviserParameters implements WithFailureTypes {
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
