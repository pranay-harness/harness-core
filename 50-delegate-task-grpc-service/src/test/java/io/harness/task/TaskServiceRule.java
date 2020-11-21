package io.harness.task;

import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class TaskServiceRule implements MethodRule, InjectorRuleMixin {
  private final ClosingFactory closingFactory;

  public TaskServiceRule(ClosingFactory closingFactory) {
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
            .add(TaskServiceTestHelper.getKryoRegistrar())
            .add(DelegateTasksBeansKryoRegister.class)
            .build();
      }
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ExecutorService.class)
            .annotatedWith(Names.named("verificationDataCollectorExecutor"))
            .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
                new ThreadFactoryBuilder()
                    .setNameFormat("Verification-Data-Collector-%d")
                    .setPriority(Thread.MIN_PRIORITY)
                    .build()));
      }
    });
    modules.add(new ClosingFactoryModule(closingFactory));

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(TaskServiceTestHelper.class).in(Singleton.class);
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
