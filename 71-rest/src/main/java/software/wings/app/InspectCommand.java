package software.wings.app;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import io.dropwizard.Application;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.govern.ProviderModule;
import io.harness.mongo.IndexManager;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InspectCommand<T extends io.dropwizard.Configuration> extends ConfiguredCommand<T> {
  private final Class<T> configurationClass;

  public InspectCommand(Application<T> application) {
    super("inspect", "Parses and validates the configuration file");
    this.configurationClass = application.getConfigurationClass();
  }

  @Override
  protected Class<T> getConfigurationClass() {
    return this.configurationClass;
  }

  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) {
    MainConfiguration mainConfiguration = (MainConfiguration) configuration;
    mainConfiguration.setMongoConnectionFactory(
        mainConfiguration.getMongoConnectionFactory().toBuilder().indexManagerMode(IndexManager.Mode.INSPECT).build());

    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        MapBinder<Class, String> morphiaClasses =
            MapBinder.newMapBinder(binder(), Class.class, String.class, Names.named("morphiaClasses"));
        morphiaClasses.addBinding(DelegateSyncTaskResponse.class).toInstance("delegateSyncTaskResponses");
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return mainConfiguration.getMongoConnectionFactory();
      }
    });
    modules.addAll(new MongoModule().cumulativeDependencies());
    modules.add(new IndexMigratorModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(ManagerRegistrars.kryoRegistrars).build();
      }
    });

    Injector injector = Guice.createInjector(modules);
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named("primaryDatastore")));
  }
}
