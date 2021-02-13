package io.harness.perpetualtask;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

/**
 * Used on the manager side to handle CRUD of a specific type of perpetual tasks.
 * @param <T> The params type of the perpetual task type being managed.
 */
@TargetModule(Module._420_DELEGATE_SERVICE)
public interface PerpetualTaskServiceInprocClient<T extends PerpetualTaskClientParams> {
  String create(String accountId, T clientParams);
}
