package io.harness.adviser.impl.ignore;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class IgnoreAdviserParameters implements AdviserParameters {
  String nextNodeId;
  Set<FailureType> applicableFailureTypes;
}
