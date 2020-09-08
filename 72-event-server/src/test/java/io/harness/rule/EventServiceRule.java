package io.harness.rule;

import static io.harness.network.LocalhostUtils.findFreePort;

import com.google.inject.Module;

import io.harness.event.app.EventServiceConfig;
import io.harness.event.app.EventServiceModule;
import io.harness.event.client.impl.appender.AppenderModule;
import io.harness.event.client.impl.tailer.TailerModule;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.grpc.server.Connector;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventServiceRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  public static final String DEFAULT_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  public static final String DEFAULT_ACCOUNT_SECRET = "2f6b0988b6fb3370073c3d0505baee59";
  public static final String DEFAULT_DELEGATE_ID = "G0yG0f9gQhKsr1xBErpUFg";

  public static final String QUEUE_FILE_PATH =
      Paths.get(FileUtils.getTempDirectoryPath(), UUID.randomUUID().toString()).toString();

  @Getter private final ClosingFactory closingFactory;

  public EventServiceRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory == null ? new ClosingFactory() : closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));

    modules.add(new AppenderModule(
        AppenderModule.Config.builder().queueFilePath(QUEUE_FILE_PATH).build(), () -> DEFAULT_DELEGATE_ID));

    int port = findFreePort();

    modules.add(new TailerModule(TailerModule.Config.builder()
                                     .accountId(DEFAULT_ACCOUNT_ID)
                                     .accountSecret(DEFAULT_ACCOUNT_SECRET)
                                     .queueFilePath(QUEUE_FILE_PATH)
                                     .publishTarget("localhost:" + port)
                                     .publishAuthority("localhost")
                                     .minDelay(Duration.ofMillis(10))
                                     .build()));

    modules.add(new EventServiceModule(
        EventServiceConfig.builder().connector(new Connector(port, true, "cert.pem", "key.pem")).build()));

    modules.add(TestMongoModule.getInstance());
    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          applyInjector(base, method, target).evaluate();
        } finally {
          closingFactory.stopServers();
        }
      }
    };
  }
}
