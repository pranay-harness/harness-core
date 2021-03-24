package io.harness;

import io.harness.entities.ApplicationCDCEntity;
import io.harness.entities.CDCEntity;
import io.harness.entities.ViewCDCEntity;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import io.harness.timescaledb.TimeScaleDBServiceImpl;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeDataCaptureModule extends AbstractModule {
  private final ChangeDataCaptureServiceConfig config;

  public ChangeDataCaptureModule(ChangeDataCaptureServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(ChangeDataCaptureServiceConfig.class).toInstance(config);
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);

    try {
      bind(TimeScaleDBService.class)
          .toConstructor(TimeScaleDBServiceImpl.class.getConstructor(TimeScaleDBConfig.class));
    } catch (NoSuchMethodException e) {
      log.error("TimeScaleDbServiceImpl Initialization Failed in due to missing constructor", e);
    }
    bind(TimeScaleDBConfig.class)
        .annotatedWith(Names.named("TimeScaleDBConfig"))
        .toInstance(config.getTimeScaleDBConfig() != null ? config.getTimeScaleDBConfig()
                                                          : TimeScaleDBConfig.builder().build());
    bindEntities();
    install(new RegistrarsModule());
  }

  private void bindEntities() {
    Multibinder<CDCEntity<?>> cdcEntityMultibinder =
        Multibinder.newSetBinder(binder(), new TypeLiteral<CDCEntity<?>>() {});
    cdcEntityMultibinder.addBinding().to(ApplicationCDCEntity.class);
//    cdcEntityMultibinder.addBinding().to(ViewCDCEntity.class);
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  public Set<Class<?>> morphiaClasses() {
    return Collections.emptySet();
  }
}
