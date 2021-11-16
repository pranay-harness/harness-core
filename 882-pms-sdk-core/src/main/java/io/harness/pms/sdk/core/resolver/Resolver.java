package io.harness.pms.sdk.core.resolver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.StepTransput;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface Resolver<T extends StepTransput> {
  T resolve(@NotNull Ambiance ambiance, @NotNull RefObject refObject);

  String consume(@NotNull Ambiance ambiance, @NotNull String name, T value, String groupName);
}
