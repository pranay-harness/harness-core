package io.harness.connector;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

import static org.mockito.Mockito.mock;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.callback.DelegateCallbackToken;
import io.harness.connector.entities.Connector;
import io.harness.connector.gitsync.ConnectorGitSyncHelper;
import io.harness.connector.impl.ConnectorActivityServiceImpl;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.factory.ClosingFactory;
import io.harness.gitsync.AbstractGitSyncSdkModule;
import io.harness.gitsync.GitSyncEntitiesConfiguration;
import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceImpl;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.persistence.HPersistence;
import io.harness.redis.RedisConfig;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.InjectorRuleMixin;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.ConnectorNextGenRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PersistenceRegistrars;
import io.harness.springdata.SpringPersistenceTestModule;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.yaml.YamlSdkModule;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.dropwizard.jackson.Jackson;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@Slf4j
public class ConnectorTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  ClosingFactory closingFactory;

  public ConnectorTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        bind(ConnectorActivityService.class).to(ConnectorActivityServiceImpl.class);
        bind(ProjectService.class).toInstance(mock(ProjectService.class));
        bind(OrganizationService.class).toInstance(mock(OrganizationService.class));
        bind(NGActivityService.class).toInstance(mock(NGActivityService.class));
        bind(SecretManagerClientService.class).toInstance(mock(SecretManagerClientService.class));
        bind(DelegateServiceGrpcClient.class).toInstance(mock(DelegateServiceGrpcClient.class));
        bind(SecretCrudService.class).toInstance(mock(SecretCrudService.class));
        bind(NGSecretManagerService.class).toInstance(mock(NGSecretManagerService.class));
        bind(Producer.class)
            .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
            .toInstance(mock(NoOpProducer.class));
        bind(Producer.class)
            .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
            .toInstance(mock(NoOpProducer.class));
        bind(new TypeLiteral<Supplier<DelegateCallbackToken>>() {
        }).toInstance(Suppliers.ofInstance(DelegateCallbackToken.newBuilder().build()));
        bind(GitAwarePersistence.class)
            .toInstance(new GitAwarePersistenceImpl<Connector, ConnectorDTO>(Connector.class, ConnectorDTO.class));
      }
    });
    modules.add(mongoTypeModule(annotations));
    modules.add(TestMongoModule.getInstance());
    modules.add(new SpringPersistenceTestModule());
    modules.add(new ConnectorModule(CEAwsSetupConfig.builder().build()));
    modules.add(KryoModule.getInstance());
    modules.add(YamlSdkModule.getInstance());
    modules.add(new EntitySetupUsageClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret", "Service"));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(ConnectorNextGenRegistrars.springConverters)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(PersistenceRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<YamlSchemaRootClass> yamlSchemaRootClass() {
        return ImmutableList.<YamlSchemaRootClass>builder()
            .addAll(ConnectorNextGenRegistrars.yamlSchemaRegistrars)
            .build();
      }

      @Provides
      @Named("yaml-schema-mapper")
      @Singleton
      public ObjectMapper getYamlSchemaObjectMapper() {
        return Jackson.newObjectMapper();
      }

      @Provides
      @Singleton
      GitAwarePersistence<Connector, ConnectorDTO> gitAwarePersistence() {
        return new GitAwarePersistenceImpl<>(Connector.class, ConnectorDTO.class);
      }
    });

    modules.add(new AbstractGitSyncSdkModule() {
      @Override
      public GitSyncSdkConfiguration getGitSyncSdkConfiguration() {
        final Supplier<List<EntityType>> sortOrder = () -> Collections.singletonList(EntityType.CONNECTORS);
        Set<GitSyncEntitiesConfiguration> gitSyncEntitiesConfigurations = new HashSet<>();
        gitSyncEntitiesConfigurations.add(GitSyncEntitiesConfiguration.builder()
                                              .yamlClass(ConnectorDTO.class)
                                              .entityClass(Connector.class)
                                              .entityHelperClass(ConnectorGitSyncHelper.class)
                                              .build());
        return GitSyncSdkConfiguration.builder()
            .gitSyncSortOrder(sortOrder)
            //            .grpcClientConfig(config.getGrpcClientConfig())
            //            .grpcServerConfig(config.getGrpcServerConfig())
            .deployMode(GitSyncSdkConfiguration.DeployMode.IN_PROCESS)
            .microservice(Microservice.CORE)
            //            .scmConnectionConfig(config.getScmConnectionConfig())
            .eventsRedisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
            .serviceHeader(NG_MANAGER)
            .gitSyncEntitiesConfiguration(gitSyncEntitiesConfigurations)
            .build();
      }
    });
    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(log, base, method, target);
  }
}
