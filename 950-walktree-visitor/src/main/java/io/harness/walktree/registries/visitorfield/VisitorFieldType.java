package io.harness.walktree.registries.visitorfield;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class VisitorFieldType {
  @NonNull String type;
}
