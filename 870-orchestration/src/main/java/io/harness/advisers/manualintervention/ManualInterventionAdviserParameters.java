package io.harness.advisers.manualintervention;

import io.harness.adviser.WithFailureTypes;
import io.harness.pms.execution.failure.FailureType;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualInterventionAdviserParameters implements WithFailureTypes {
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
