package io.harness.supplier;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
@FunctionalInterface
public interface ThrowingSupplier<T> {
  T get() throws Exception;
}
