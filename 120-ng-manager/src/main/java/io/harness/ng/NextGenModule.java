package io.harness.ng;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.connector.ConnectorModule;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.SecretManagementModule;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class NextGenModule extends AbstractModule {
  private final NextGenConfiguration appConfig;

  public NextGenModule(NextGenConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    bind(NextGenConfiguration.class).toInstance(appConfig);

    install(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getMongoConfig();
      }
    });

    /*
    [secondary-db]: To use another DB, uncomment this and add @Named("primaryMongoConfig") to the above one

    install(new ProviderModule() {
       @Provides
       @Singleton
       @Named("secondaryMongoConfig")
       MongoConfig mongoConfig() {
         return appConfig.getSecondaryMongoConfig();
       }
     });*/
    install(new ValidationModule(getValidatorFactory()));
    install(new NextGenPersistenceModule());
    install(new CoreModule());
    install(new ConnectorModule());
    install(new SecretManagementModule(this.appConfig.getSecretManagerClientConfig()));
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }
}
