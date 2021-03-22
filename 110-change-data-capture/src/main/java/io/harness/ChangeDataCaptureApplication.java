package io.harness;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.govern.ProviderModule;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaModule;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.Store;
import io.harness.persistence.UserProvider;
import io.harness.serializer.PersistenceRegistrars;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.converters.TypeConverter;

@Slf4j
public class ChangeDataCaptureApplication extends Application<ChangeDataCaptureServiceConfig> {
  public static final String EVENTS_DB = "events";
  public static final Store EVENTS_STORE = Store.builder().name(EVENTS_DB).build();
  public static final String CDC_DB = "change-data-capture";
  public static final Store CDC_STORE = Store.builder().name(CDC_DB).build();

  public static void main(String[] args) throws Exception {
    log.info("Starting Change Data Capture Application...");

    new ChangeDataCaptureApplication().run(args);
  }

  @Override
  public void run(ChangeDataCaptureServiceConfig changeDataCaptureServiceConfig, Environment environment)
      throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return changeDataCaptureServiceConfig.getHarnessMongo();
      }
    });

    modules.add(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .put(DelegateTaskProgressResponse.class, "delegateTaskProgressResponses")
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(PersistenceRegistrars.morphiaConverters)
            .build();
      }
    });

    modules.add(MorphiaModule.getInstance());
    modules.add(new ChangeDataCaptureModule(changeDataCaptureServiceConfig));

    Injector injector = Guice.createInjector(modules);
    registerStores(changeDataCaptureServiceConfig, injector);
    registerManagedBeans(environment, injector);
  }

  private static void registerStores(ChangeDataCaptureServiceConfig config, Injector injector) {
    final String eventsMongoUri = config.getEventsMongo().getUri();
    final String cdcMongoUri = config.getCdcMongo().getUri();
    if (isNotEmpty(eventsMongoUri) && !eventsMongoUri.equals(config.getHarnessMongo().getUri())) {
      final HPersistence hPersistence = injector.getInstance(HPersistence.class);
      hPersistence.register(EVENTS_STORE, config.getEventsMongo().getUri());
    }
    if (isNotEmpty(cdcMongoUri) && !cdcMongoUri.equals(config.getHarnessMongo().getUri())) {
      final HPersistence hPersistence = injector.getInstance(HPersistence.class);
      hPersistence.register(CDC_STORE, config.getCdcMongo().getUri());
    }
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(ChangeDataCaptureSyncService.class));
  }
}
