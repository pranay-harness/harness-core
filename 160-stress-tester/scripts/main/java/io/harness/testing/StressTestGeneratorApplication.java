package io.harness.testing;

import io.harness.govern.ProviderModule;
import io.harness.serializer.DelegateServiceBeansRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StressTestGeneratorApplication {
  public static void main(String... args) {
    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar> > registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar> >builder()
            .addAll(DelegateServiceBeansRegistrars.kryoRegistrars)
            .build();
      }
    });
    Injector injector = Guice.createInjector(modules);

    StressTestGenerator stressTestGenerator = injector.getInstance(CapabilityStressTestGenerator.class);
    System.out.println(stressTestGenerator.makeStressTest());
  }
}
