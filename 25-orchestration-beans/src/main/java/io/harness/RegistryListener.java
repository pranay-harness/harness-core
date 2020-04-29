package io.harness;

import static org.joor.Reflect.on;

import com.google.inject.spi.ProvisionListener;

import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;
import io.harness.registries.Registry;
import org.reflections.Reflections;

import java.util.List;
import java.util.stream.Collectors;

@Redesign
public class RegistryListener implements ProvisionListener {
  @Override
  public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
    T provision = provisionInvocation.provision();
    if (!(provision instanceof Registry)) {
      return;
    }
    Reflections reflections = new Reflections(OrchestrationBeansModule.class.getPackage().getName(), "software.wings");
    Registry registry = (Registry) provision;
    List<Class<?>> allClasses = reflections.getTypesAnnotatedWith(Produces.class)
                                    .stream()
                                    .filter(clazz -> registry.getRegistrableEntityClass().isAssignableFrom(clazz))
                                    .collect(Collectors.toList());
    for (Class clazz : allClasses) {
      RegistrableEntity entity = on(clazz).create().get();
      registry.register(entity.getType(), entity);
    }
  }
}
