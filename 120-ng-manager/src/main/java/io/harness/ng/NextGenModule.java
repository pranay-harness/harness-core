package io.harness.ng;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriverModule;
import io.harness.connector.ConnectorModule;
import io.harness.govern.DependencyModule;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.NgManagerGrpcServerModule;
import io.harness.ng.core.SecretManagementModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import io.harness.version.VersionModule;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class NextGenModule extends DependencyModule {
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
    install(new SecretManagementModule(
        this.appConfig.getSecretManagerClientConfig(), this.appConfig.getNextGenConfig().getManagerServiceSecret()));
    install(new ManagerDelegateServiceDriverModule(this.appConfig.getGrpcClientConfig(),
        this.appConfig.getNextGenConfig().getManagerServiceSecret(), NextGenConfiguration.SERVICE_ID));
    install(new NgManagerGrpcServerModule(
        this.appConfig.getGrpcServerConfig(), this.appConfig.getNextGenConfig().getManagerServiceSecret()));
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(NextGenRegistrars.kryoRegistrars).build();
      }
    });
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(VersionModule.getInstance());
  }
}
