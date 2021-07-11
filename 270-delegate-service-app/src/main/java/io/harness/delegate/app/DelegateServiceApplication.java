package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;

import io.harness.annotations.dev.TargetModule;
import io.harness.govern.ProviderModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.DelegateServiceDriverRegistrars;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.KryoRegistrar;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.stream.GuiceObjectFactory;
import io.harness.stream.StreamModule;

import software.wings.jersey.JsonViews;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
import org.mongodb.morphia.converters.TypeConverter;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
public class DelegateServiceApplication extends Application<DelegateServiceConfig> {
  public static void main(String... args) throws Exception {
    new DelegateServiceApplication().run(args);
  }

  @Override
  public void run(DelegateServiceConfig delegateServiceConfig, Environment environment) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new DelegateServiceModule(delegateServiceConfig));

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AtmosphereBroadcaster atmosphereBroadcaster() {
        return delegateServiceConfig.getAtmosphereBroadcaster();
      }
    });
    modules.add(StreamModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
            .build();
      }
      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }
    });
    modules.add(new io.harness.mongo.AbstractMongoModule() {
      @Override
      public io.harness.persistence.UserProvider userProvider() {
        return new io.harness.persistence.NoopUserProvider();
      }

      @Provides
      @Singleton
      io.harness.mongo.MongoConfig mongoConfig() {
        return io.harness.mongo.MongoConfig.builder().build();
      }
    });

    Injector injector = Guice.createInjector(modules);

    registerAtmosphereStreams(environment, injector);
    initializegRPCServer(injector);
  }

  @Override
  public String getName() {
    return "Delegate Service Application";
  }

  @Override
  public void initialize(Bootstrap<DelegateServiceConfig> bootstrap) {
    log.info("Initialize - start bootstrap ");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.getObjectMapper().setSubtypeResolver(
        new JsonSubtypeResolver(bootstrap.getObjectMapper().getSubtypeResolver()));
    bootstrap.getObjectMapper().setConfig(
        bootstrap.getObjectMapper().getSerializationConfig().withView(JsonViews.Public.class));
  }

  private void registerAtmosphereStreams(Environment environment, Injector injector) {
    AtmosphereServlet atmosphereServlet = injector.getInstance(AtmosphereServlet.class);
    atmosphereServlet.framework().objectFactory(new GuiceObjectFactory(injector));
    injector.getInstance(BroadcasterFactory.class);
    injector.getInstance(MetaBroadcaster.class);
    ServletRegistration.Dynamic dynamic = environment.servlets().addServlet("StreamServlet", atmosphereServlet);
    dynamic.setAsyncSupported(true);
    dynamic.setLoadOnStartup(0);
    dynamic.addMapping("/stream/*");
  }

  private void initializegRPCServer(Injector injector) {
    log.info("Initializing gRPC Server on Delegate Service application");
    injector.getInstance(ServiceManager.class);
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
  }
}
