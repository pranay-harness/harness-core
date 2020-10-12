package io.harness.advisers.manualintervention;

import io.harness.adviser.AdviserParameters;
import io.harness.adviser.advise.WithFailureTypes;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;
import java.util.Set;

@Value
@Builder
public class ManualInterventionAdviserParameters implements AdviserParameters, WithFailureTypes {
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
