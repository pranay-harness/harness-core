package io.harness.rule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.factory.ClosingFactory;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ModelRule implements MethodRule, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(ModelRule.class);

  private Injector injector;
  private ClosingFactory closingFactory = new ClosingFactory();

  protected void before() {
    List<Module> modules = new ArrayList<>();

    injector = Guice.createInjector(modules);
  }
  protected void after() {
    closingFactory.stopServers();
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        before();
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          after();
        }
      }
    };
  }
}
