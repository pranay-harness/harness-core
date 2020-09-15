package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.runners.ModuleListProvider;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationBeansRegistrars;
import io.harness.serializer.kryo.OrchestrationBeansTestKryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.serializer.morphia.TestPersistenceMorphiaRegistrar;
import io.harness.spring.AliasRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OrchestrationBeansModuleListProvider implements ModuleListProvider {
  public List<Module> modules() {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
            .add(TestPersistenceKryoRegistrar.class)
            .add(OrchestrationBeansTestKryoRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
            .add(TestPersistenceMorphiaRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(OrchestrationBeansRegistrars.aliasRegistrars)
            .build();
      }
    });
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
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
    modules.add(TestMongoModule.getInstance());
    modules.add(OrchestrationBeansModule.getInstance());
    return modules;
  }
}
