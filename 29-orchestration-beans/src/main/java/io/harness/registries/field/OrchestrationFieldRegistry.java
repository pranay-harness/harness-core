package io.harness.registries.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;
import lombok.NonNull;

@OwnedBy(CDC)
@Redesign
@Singleton
public class OrchestrationFieldRegistry implements Registry<OrchestrationFieldType, OrchestrationFieldProcessor> {
  private Map<OrchestrationFieldType, OrchestrationFieldProcessor> registry = new ConcurrentHashMap<>();

  public void register(
      @NonNull OrchestrationFieldType orchestrationFieldType, @NonNull OrchestrationFieldProcessor processor) {
    if (registry.containsKey(orchestrationFieldType)) {
      throw new DuplicateRegistryException(
          getType(), "Orchestration Field Processor Already Registered with this type: " + orchestrationFieldType);
    }
    registry.put(orchestrationFieldType, processor);
  }

  public OrchestrationFieldProcessor<?> obtain(@Valid OrchestrationFieldType orchestrationFieldType) {
    if (registry.containsKey(orchestrationFieldType)) {
      return registry.get(orchestrationFieldType);
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No Orchestration Field Processor registered for type: " + orchestrationFieldType);
  }

  @Override
  public String getType() {
    return RegistryType.ORCHESTRATION_FIELD.name();
  }
}
