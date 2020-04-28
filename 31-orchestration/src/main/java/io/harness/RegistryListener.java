package io.harness;

import static org.joor.Reflect.on;

import com.google.inject.spi.ProvisionListener;

import io.harness.adviser.Adviser;
import io.harness.ambiance.Level;
import io.harness.annotations.Produces;
import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.facilitate.Facilitator;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.State;
import org.reflections.Reflections;

import java.util.Set;

@Redesign
public class RegistryListener implements ProvisionListener {
  @Override
  public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
    T provision = provisionInvocation.provision();
    if (!(provision instanceof Registry)) {
      return;
    }

    Reflections reflections = new Reflections(OrchestrationModule.class.getPackage().getName());
    Registry registry = (Registry) provision;
    Set<Class<?>> allClasses = reflections.getTypesAnnotatedWith(Produces.class);
    allClasses.forEach(clazz -> registerClass(registry, clazz));
  }

  private void registerClass(Registry registry, Class<?> clazz) {
    RegistryType registryType = registry.getType();
    Produces annotation = clazz.getAnnotation(Produces.class);
    Class<?> annotationClass = annotation.value();
    switch (registryType) {
      case STATE:
        if (annotationClass == State.class) {
          State state = on(clazz).create().get();
          ((StateRegistry) registry).register(state.getType(), state);
        }
        break;
      case ADVISER:
        if (annotationClass == Adviser.class) {
          Adviser adviser = on(clazz).create().get();
          ((AdviserRegistry) registry).register(adviser.getType(), adviser);
        }
        break;
      case RESOLVER:
        if (annotationClass == Resolver.class) {
          Resolver resolver = on(clazz).create().get();
          ((ResolverRegistry) registry).register(resolver.getType(), resolver);
        }
        break;
      case FACILITATOR:
        if (annotationClass == Facilitator.class) {
          Facilitator facilitator = on(clazz).create().get();
          ((FacilitatorRegistry) registry).register(facilitator.getType(), facilitator);
        }
        break;
      case LEVEL:
        if (annotationClass == Level.class) {
          Level level = on(clazz).create().get();
          ((LevelRegistry) registry).register(level);
        }
        break;
      default:
        throw new InvalidRequestException("Registry does not exist for type: " + registryType);
    }
  }
}
