package io.harness.app.impl;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.app.CIManagerConfiguration;
import io.harness.app.CIManagerServiceModule;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.queue.QueueController;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.ApiServiceKryoRegister;
import io.harness.serializer.kryo.CIBeansRegistrar;
import io.harness.serializer.kryo.CIExecutionRegistrar;
import io.harness.serializer.kryo.CVNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.CommonsKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentKryoRegister;
import io.harness.serializer.kryo.DelegateKryoRegister;
import io.harness.serializer.kryo.DelegateTasksKryoRegister;
import io.harness.serializer.kryo.ManagerKryoRegistrar;
import io.harness.serializer.kryo.NGKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationBeansKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationKryoRegister;
import io.harness.serializer.kryo.PersistenceRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CIManagerRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public CIManagerRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .add(ApiServiceKryoRegister.class)
            .add(CIBeansRegistrar.class)
            .add(CIExecutionRegistrar.class)
            .add(CommonsKryoRegistrar.class)
            .add(CVNextGenCommonsBeansKryoRegistrar.class)
            .add(DelegateAgentKryoRegister.class)
            .add(DelegateKryoRegister.class)
            .add(DelegateTasksKryoRegister.class)
            .add(ManagerKryoRegistrar.class)
            .add(NGKryoRegistrar.class)
            .add(OrchestrationBeansKryoRegistrar.class)
            .add(OrchestrationKryoRegister.class)
            .add(PersistenceRegistrar.class)
            .add(TestPersistenceKryoRegistrar.class)
            .build();
      }
    });

    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));
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
    modules.add(new CIManagerPersistenceTestModule());
    modules.addAll(new CIManagerServiceModule(CIManagerConfiguration.builder().build(), null).cumulativeDependencies());
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