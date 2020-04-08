package io.harness.adviser.impl.ignore;

import io.harness.adviser.AdviserParameters;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class IgnoreAdviserParameters implements AdviserParameters {
  String nextNodeId;
  Set<FailureType> applicableFailureTypes;
}
