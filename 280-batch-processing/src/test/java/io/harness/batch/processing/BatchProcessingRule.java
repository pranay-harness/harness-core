package io.harness.batch.processing;

import static org.mockito.Mockito.mock;

import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

import io.harness.batch.processing.config.BatchProcessingRegistrarsModule;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.persistence.HPersistence;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchProcessingRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  private final ClosingFactory closingFactory;

  public BatchProcessingRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          applyInjector(base, method, target).evaluate();
        } finally {
          closingFactory.close();
        }
      }
    };
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));
    modules.add(TestMongoModule.getInstance());
    modules.add(new BatchProcessingRegistrarsModule());
    Module overriden = Modules.override(modules).with(new ProviderModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(WingsMongoPersistence.class);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
        bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
      }

      @Provides
      @Singleton
      public TimeScaleDBService timeScaleDBService() {
        // TODO: Fix this when we have a proper timescale db in tests.
        return mock(TimeScaleDBService.class);
      }
    });
    return Collections.singletonList(overriden);
  }
}
