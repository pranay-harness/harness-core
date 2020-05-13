package io.harness.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import io.dropwizard.Application;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.harness.govern.ProviderModule;
import io.harness.mongo.IndexManager;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.ArrayList;
import java.util.List;

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
    VerificationServiceConfiguration verificationServiceConfiguration =
        (VerificationServiceConfiguration) configuration;
    verificationServiceConfiguration.setMongoConnectionFactory(
        verificationServiceConfiguration.getMongoConnectionFactory()
            .toBuilder()
            .indexManagerMode(IndexManager.Mode.INSPECT)
            .build());

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return verificationServiceConfiguration.getMongoConnectionFactory();
      }
    });
    modules.addAll(new MongoModule().cumulativeDependencies());
    Injector injector = Guice.createInjector(modules);
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named("primaryDatastore")));
  }
}
