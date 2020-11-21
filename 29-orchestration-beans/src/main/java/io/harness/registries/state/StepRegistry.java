package io.harness.registries.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.steps.StepType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;
import io.harness.state.Step;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class StepRegistry implements Registry<StepType, Step> {
  Map<StepType, Step> registry = new ConcurrentHashMap<>();

  public void register(@NonNull StepType stepType, @NonNull Step step) {
    if (registry.containsKey(stepType)) {
      throw new DuplicateRegistryException(getType(), "Step Already Registered with this type: " + stepType);
    }
    registry.put(stepType, step);
  }

  public Step obtain(@NonNull StepType stepType) {
    if (registry.containsKey(stepType)) {
      return registry.get(stepType);
    }
    throw new UnregisteredKeyAccessException(getType(), "No Step registered for type: " + stepType);
  }

  @Override
  public String getType() {
    return RegistryType.STEP.name();
  }
}
