package io.harness.executionplan.rule;

import static org.mockito.Mockito.mock;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.CIExecutionServiceModule;
import io.harness.CIExecutionTestModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.ngpipeline.inputset.repository.spring.InputSetRepository;
import io.harness.ngpipeline.pipeline.repository.spring.NgPipelineRepository;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.CiExecutionRegistrars;
import io.harness.spring.AliasRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Initiates mongo connection and register classes for running UTs
 */

public class CIExecutionRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;
  @Rule public CIExecutionTestModule testRule = new CIExecutionTestModule();
  public CIExecutionRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));
    modules.add(new CIExecutionTestModule());
    modules.add(new EntitySetupUsageClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret"));
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(NgPipelineRepository.class).toInstance(mock(NgPipelineRepository.class));
        bind(InputSetRepository.class).toInstance(mock(InputSetRepository.class));
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

    modules.add(new AbstractModule() {
      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(CiExecutionRegistrars.aliasRegistrars)
            .build();
      }
    });
    modules.add(TestMongoModule.getInstance());
    modules.add(new CIExecutionPersistenceTestModule());
    modules.add(new CIExecutionServiceModule());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<Supplier<DelegateCallbackToken>>() {})
            .toInstance(Suppliers.ofInstance(DelegateCallbackToken.newBuilder().build()));
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
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
