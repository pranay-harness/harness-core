package io.harness.delegate.app.modules;

import io.harness.govern.ProviderModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;

public class DelegateKryoModule extends ProviderModule {
  @Provides
  @Singleton
  Set<Class<? extends KryoRegistrar> > registrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar> >builder().addAll(ManagerRegistrars.kryoRegistrars).build();
  }
}
