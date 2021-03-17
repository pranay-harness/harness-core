package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.contracts.execution.failure.FailureType;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Wither;

@Data
@Builder
public class RollbackInfo {
  RollbackStrategy strategy;
  Set<FailureType> failureTypes;
  @Singular("nodeTypeToUuid") Map<String, String> nodeTypeToUuid;
  @Wither String identifier;
  @Wither String group;

  public RollbackInfo getRollbackInfo(String identifier, String group) {
    return this.withIdentifier(identifier).withGroup(group);
  }
}
