package io.harness.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LoggingInitializer.initializeLogging;

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
import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import org.apache.log4j.LogManager;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.slf4j.Logger;
import ru.vyarus.guice.validator.ValidationModule;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class CIManagerApplication extends Application<CIManagerConfiguration> {
  public static final String CI_DB = "harnessci";
  public static final Store CI_STORE = Store.builder().name(CI_DB).build();
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CIManagerApplication.class);
  private static String APPNAME = "CI Manager Service Application";

  public static void main(String[] args) throws Exception {
    new CIManagerApplication().run(args);
  }

  @Override
  public String getName() {
    return APPNAME;
  }
  @Override
  public void run(CIManagerConfiguration configuration, Environment environment) {
    logger.info("Starting ci manager app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    logger.info("Leaving startup maintenance mode");
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getHarnessMongo();
      }
    });

    modules.addAll(new MongoModule().cumulativeDependencies());
    addGuiceValidationModule(modules);
    modules.add(new CIManagerServiceModule(configuration));

    Injector injector = Guice.createInjector(modules);

    registerStores(configuration, injector);
    logger.info("Starting app done");
    MaintenanceController.resetForceMaintenance();
    LogManager.shutdown();
  }

  @Override
  public void initialize(Bootstrap<CIManagerConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");

    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    logger.info("bootstrapping done.");
  }

  private static void addGuiceValidationModule(List<Module> modules) {
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    modules.add(new ValidationModule(validatorFactory));
  }

  private static void registerStores(CIManagerConfiguration config, Injector injector) {
    final String ciMongo = config.getHarnessCIMongo().getUri();
    if (isNotEmpty(ciMongo) && !ciMongo.equals(config.getHarnessMongo().getUri())) {
      final HPersistence hPersistence = injector.getInstance(HPersistence.class);
      hPersistence.register(CI_STORE, config.getHarnessCIMongo().getUri());
    }
  }
}
