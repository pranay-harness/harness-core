package io.harness;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.ChangeDataCaptureRegistrars;
import io.harness.serializer.KryoRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Set;

public class RegistrarsModule extends AbstractModule {
  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ChangeDataCaptureRegistrars.kryoRegistrars;
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ChangeDataCaptureRegistrars.morphiaRegistrars;
  }
}
