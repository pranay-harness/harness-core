package io.harness.advisers.fail;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.advise.WithFailureTypes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;
import java.util.Set;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class OnFailAdviserParameters implements WithFailureTypes {
  String nextNodeId;
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
