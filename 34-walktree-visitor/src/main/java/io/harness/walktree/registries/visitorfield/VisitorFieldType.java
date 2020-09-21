package io.harness.walktree.registries.visitorfield;

import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class VisitorFieldType implements RegistryKey {
  @NonNull String type;
}
