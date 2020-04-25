package io.harness;

import static org.joor.Reflect.on;

import com.google.inject.spi.ProvisionListener;

import io.harness.annotations.ProducesAdviser;
import io.harness.annotations.ProducesFacilitator;
import io.harness.annotations.ProducesLevel;
import io.harness.annotations.ProducesResolver;
import io.harness.annotations.ProducesState;
import io.harness.annotations.Redesign;
import io.harness.exception.InvalidRequestException;
import io.harness.registries.Registry;
import io.harness.registries.RegistryType;
import io.harness.registries.adviser.AdviserProducer;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorProducer;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.level.LevelRegistry;
import io.harness.registries.resolver.ResolverProducer;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StateProducer;
import io.harness.registries.state.StateRegistry;
import io.harness.state.io.ambiance.Level;
import org.reflections.Reflections;

import java.util.Set;

@Redesign
public class RegistryListener implements ProvisionListener {
  @Override
  public <T> void onProvision(ProvisionInvocation<T> provisionInvocation) {
    T provision = provisionInvocation.provision();
    if (provision instanceof Registry) {
      Reflections reflections = new Reflections(OrchestrationModule.class.getPackage().getName());
      RegistryType registryType = ((Registry) provision).getType();
      Set<Class<? extends Object>> allClasses;
      switch (registryType) {
        case STATE:
          allClasses = reflections.getTypesAnnotatedWith(ProducesState.class);
          for (Class clazz : allClasses) {
            StateProducer producer = on(clazz).create().get();
            ((StateRegistry) provision).register(producer.getType(), producer);
          }
          break;
        case ADVISER:
          allClasses = reflections.getTypesAnnotatedWith(ProducesAdviser.class);
          for (Class clazz : allClasses) {
            AdviserProducer producer = on(clazz).create().get();
            ((AdviserRegistry) provision).register(producer.getType(), producer);
          }
          break;
        case RESOLVER:
          allClasses = reflections.getTypesAnnotatedWith(ProducesResolver.class);
          for (Class clazz : allClasses) {
            ResolverProducer producer = on(clazz).create().get();
            ((ResolverRegistry) provision).register(producer.getType(), producer);
          }
          break;
        case FACILITATOR:
          allClasses = reflections.getTypesAnnotatedWith(ProducesFacilitator.class);
          for (Class clazz : allClasses) {
            FacilitatorProducer producer = on(clazz).create().get();
            ((FacilitatorRegistry) provision).register(producer.getType(), producer);
          }
          break;
        case LEVEL:
          allClasses = reflections.getTypesAnnotatedWith(ProducesLevel.class);
          for (Class clazz : allClasses) {
            Level level = on(clazz).create().get();
            ((LevelRegistry) provision).register(level);
          }
          break;
        default:
          throw new InvalidRequestException("Registry did not exist for type :" + registryType);
      }
    }
  }
}
