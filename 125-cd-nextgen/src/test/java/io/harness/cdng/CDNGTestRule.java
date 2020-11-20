package io.harness.cdng;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static org.mockito.Mockito.mock;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationVisualizationModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.connector.services.ConnectorService;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.rule.InjectorRuleMixin;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.CDNGRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.spring.AliasRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mongodb.morphia.converters.TypeConverter;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class CDNGTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  ClosingFactory closingFactory;

  public CDNGTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(CDNGRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CDNGRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder().addAll(CDNGRegistrars.aliasRegistrars).build();
      }

      @Provides
      @Singleton
      public OrchestrationModuleConfig orchestrationModuleConfig() {
        return OrchestrationModuleConfig.builder()
            .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(ManagerRegistrars.morphiaConverters)
            .build();
      }
    });
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        bind(ConnectorService.class)
            .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
            .toInstance(Mockito.mock(ConnectorService.class));
        bind(SecretManagerClientService.class).toInstance(mock(SecretManagerClientService.class));
        bind(DelegateServiceGrpcClient.class).toInstance(mock(DelegateServiceGrpcClient.class));
        bind(EntitySetupUsageClient.class).toInstance(mock(EntitySetupUsageClient.class));
        bind(new TypeLiteral<Supplier<DelegateCallbackToken>>() {})
            .toInstance(Suppliers.ofInstance(DelegateCallbackToken.newBuilder().build()));
      }
    });
    modules.add(TimeModule.getInstance());
    modules.add(NGModule.getInstance());
    modules.add(TestMongoModule.getInstance());
    modules.add(new CDNGPersistenceTestModule());
    modules.add(OrchestrationModule.getInstance());
    modules.add(ExecutionPlanModule.getInstance());
    modules.add(mongoTypeModule(annotations));
    modules.add(OrchestrationVisualizationModule.getInstance());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
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
    return applyInjector(base, method, target);
  }
}
