package io.harness.advisers.ignore;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.advise.WithFailureTypes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.failure.FailureType;

import java.util.EnumSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@TypeAlias("ignoreAdviserParameters")
public class IgnoreAdviserParameters implements WithFailureTypes {
  String nextNodeId;
  @Builder.Default Set<FailureType> applicableFailureTypes = EnumSet.noneOf(FailureType.class);
}
