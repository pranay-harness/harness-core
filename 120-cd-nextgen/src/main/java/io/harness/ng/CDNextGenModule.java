package io.harness.ng;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.CoreModule;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class CDNextGenModule extends AbstractModule {
  private final CDNextGenConfiguration appConfig;

  public CDNextGenModule(CDNextGenConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  protected void configure() {
    bind(CDNextGenConfiguration.class).toInstance(appConfig);

    install(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getMongoConfig();
      }
    });
    install(new ValidationModule(getValidatorFactory()));
    install(new PersistenceModule());
    install(new CoreModule());
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }
}
