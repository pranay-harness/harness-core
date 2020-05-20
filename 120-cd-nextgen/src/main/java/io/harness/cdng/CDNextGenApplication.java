package io.harness.cdng;

import static io.harness.cdng.CDNextGenConfiguration.getResourceClasses;
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
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.springframework.guice.module.BeanFactoryProvider;
import org.springframework.guice.module.SpringModule;
import ru.vyarus.guice.validator.ValidationModule;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class CDNextGenApplication extends Application<CDNextGenConfiguration> {
  private static final String APPLICATION_NAME = "CD NextGen Application";

  public static void main(String[] args) throws Exception {
    new CDNextGenApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<CDNextGenConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new SwaggerBundle<CDNextGenConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(CDNextGenConfiguration appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
  }

  @Override
  public void run(CDNextGenConfiguration appConfig, Environment environment) {
    logger.info("Starting CD Next Gen Application ...");

    Injector injector = Guice.createInjector(getModules(appConfig));

    registerResources(environment, injector);
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private Iterable<? extends Module> getModules(CDNextGenConfiguration appConfig) {
    List<Module> modules = new ArrayList<>();

    modules.add(new CDNextGenModule());
    modules.add(new ValidationModule(getValidatorFactory()));
    modules.addAll(new MongoModule().cumulativeDependencies());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getMongoConfig();
      }
    });
    modules.add(new SpringModule(BeanFactoryProvider.from(CDNextGenSpringConfiguration.class)));

    return modules;
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }
}
