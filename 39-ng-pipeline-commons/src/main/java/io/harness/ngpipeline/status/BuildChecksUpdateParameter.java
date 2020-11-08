package io.harness.ngpipeline.status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildChecksUpdateParameter implements BuildUpdateParameters {
  private final BuildUpdateType buildUpdateType = BuildUpdateType.CHECKS;
}
