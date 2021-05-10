package io.harness.ngpipeline.status;

import io.harness.annotations.dev.ToBeDeleted;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ToBeDeleted
@Deprecated
public class BuildChecksUpdateParameter implements BuildUpdateParameters {
  private final BuildUpdateType buildUpdateType = BuildUpdateType.CHECKS;
}
