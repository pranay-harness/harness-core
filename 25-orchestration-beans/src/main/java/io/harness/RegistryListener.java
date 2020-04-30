package io.harness;

import static org.joor.Reflect.on;

import com.google.inject.spi.ProvisionListener;

import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.registries.RegistrableEntity;
import io.harness.registries.Registry;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Redesign
public class RegistryListener implements ProvisionListener {
  @Override
  public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
    T provision = provisionInvocation.provision();
    if (!(provision instanceof Registry)) {
      return;
    }
    Registry registry = (Registry) provision;
    long startTime = System.currentTimeMillis();
    logger.info("Registry Loading Starting got registry type {} at {}", registry.getType(), startTime);
    Reflections reflections = new Reflections(OrchestrationBeansModule.class.getPackage().getName());
    List<Class<?>> allClasses = reflections.getTypesAnnotatedWith(Produces.class)
                                    .stream()
                                    .filter(clazz -> registry.getRegistrableEntityClass().isAssignableFrom(clazz))
                                    .collect(Collectors.toList());
    for (Class clazz : allClasses) {
      RegistrableEntity entity = on(clazz).create().get();
      registry.register(entity.getType(), entity);
    }
    logger.info("Registry Loading Ended for registry type {} took {}ms", registry.getType(),
        System.currentTimeMillis() - startTime);
  }
}
