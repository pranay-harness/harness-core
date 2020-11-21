package io.harness.orchestration;

import io.harness.spring.AliasRegistrar;
import io.harness.testing.TestExecution;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import java.util.Set;

public class OrchestrationPersistenceModule extends AbstractModule {
  private static OrchestrationPersistenceModule instance;

  public static synchronized OrchestrationPersistenceModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationPersistenceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Provider<Set<Class<? extends AliasRegistrar>>> providerClasses =
        getProvider(Key.get(new TypeLiteral<Set<Class<? extends AliasRegistrar>>>() {}));
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("Orchestration Alias Registrar Tests")
        .toInstance(() -> OrchestrationAliasUtils.validateModule(providerClasses));
  }
}
