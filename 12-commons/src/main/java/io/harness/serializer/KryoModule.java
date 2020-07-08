package io.harness.serializer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

import io.harness.govern.DependencyModule;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.Set;

@Slf4j
public class KryoModule extends DependencyModule {
  private static volatile KryoModule instance;

  public static KryoModule getInstance() {
    if (instance == null) {
      instance = new KryoModule();
    }
    return instance;
  }

  private boolean inSpring;

  public KryoModule() {
    inSpring = false;
  }

  public KryoModule(boolean inSpring) {
    this.inSpring = inSpring;
  }

  public void testAutomaticSearch(Provider<Set<Class<? extends KryoRegistrar>>> registrarsProvider) {
    Reflections reflections = new Reflections("io.harness.serializer.kryo");

    // Reflections have race issue and rarely but form time to time returns less.
    // We are checking here only if we missed something, not exact match on purpose
    Set<Class<? extends KryoRegistrar>> reflectionRegistrars = reflections.getSubTypesOf(KryoRegistrar.class);

    Set<Class<? extends KryoRegistrar>> registrars = registrarsProvider.get();

    reflectionRegistrars.removeAll(registrars);
    if (isNotEmpty(reflectionRegistrars)) {
      throw new IllegalStateException(String.format("You are missing %s", reflectionRegistrars));
    }
  }

  @Override
  protected void configure() {
    // Dummy kryo initialization trigger to make sure it is in good condition
    KryoUtils.asBytes(1);

    if (!inSpring) {
      Provider<Set<Class<? extends KryoRegistrar>>> provider =
          getProvider(Key.get(new TypeLiteral<Set<Class<? extends KryoRegistrar>>>() {}));
      MapBinder<String, TestExecution> testExecutionMapBinder =
          MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
      testExecutionMapBinder.addBinding("Kryo test registration").toInstance(() -> testAutomaticSearch(provider));
    }
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
