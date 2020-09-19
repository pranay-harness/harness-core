package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.runners.ModuleListProvider;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationRegistrars;
import io.harness.serializer.kryo.OrchestrationTestKryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.serializer.morphia.TestPersistenceMorphiaRegistrar;
import io.harness.serializer.spring.OrchestrationTestSpringAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OrchestrationModuleListProvider implements ModuleListProvider {
  public List<Module> modules() {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(OrchestrationRegistrars.kryoRegistrars)
            .add(TestPersistenceKryoRegistrar.class)
            .add(OrchestrationTestKryoRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(OrchestrationRegistrars.morphiaRegistrars)
            .add(TestPersistenceMorphiaRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(OrchestrationRegistrars.aliasRegistrars)
            .add(OrchestrationTestSpringAliasRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      public OrchestrationModuleConfig orchestrationModuleConfig() {
        return OrchestrationModuleConfig.builder()
            .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
            .build();
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoRuleMixin.MongoType provideMongoType() {
        return MongoRuleMixin.MongoType.REAL;
      }
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });

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
    modules.add(new VersionModule());
    modules.add(TimeModule.getInstance());
    modules.add(new OrchestrationPersistenceTestModule());
    modules.add(OrchestrationModule.getInstance());
    return modules;
  }
}
