package io.harness.batch.processing.k8s.rcd;

import lombok.Builder;
import lombok.Value;

import javax.annotation.Nonnull;

@Value
@Builder
public class ResourceClaim {
  public static final ResourceClaim EMPTY = ResourceClaim.builder().build();

  long cpuNano;
  long memBytes;

  public ResourceClaim scale(int n) {
    return ResourceClaim.builder().cpuNano(cpuNano * n).memBytes(memBytes * n).build();
  }

  public ResourceClaim minus(@Nonnull ResourceClaim that) {
    return ResourceClaim.builder().cpuNano(cpuNano - that.cpuNano).memBytes(memBytes - that.memBytes).build();
  }
}
