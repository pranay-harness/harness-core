package io.harness.event.app;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.event.client.EventPublisher;
import io.harness.event.client.impl.appender.AppenderModule;
import io.harness.event.client.impl.tailer.ChronicleEventTailer;
import io.harness.event.client.impl.tailer.TailerModule;
import io.harness.factory.ClosingFactory;
import io.harness.grpc.server.Connector;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.rule.InjectorRuleMixin;
import io.harness.rule.MongoRuleMixin;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import software.wings.security.ThreadLocalUserProvider;

import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class EventServiceTestRule implements MethodRule, MongoRuleMixin, InjectorRuleMixin {
  static final String DEFAULT_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  static final String DEFAULT_ACCOUNT_SECRET = "2f6b0988b6fb3370073c3d0505baee59";
  static final String DEFAULT_DELEGATE_ID = "G0yG0f9gQhKsr1xBErpUFg";

  private static final String QUEUE_FILE_PATH =
      Paths.get(FileUtils.getTempDirectoryPath(), UUID.randomUUID().toString()).toString();
  private static final int PORT = 9890;

  @Getter private final ClosingFactory closingFactory = new ClosingFactory();

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoInfo.getClient(), databaseName());
    datastore.setQueryFactory(new QueryFactory());
    return ImmutableList.of(new AppenderModule(AppenderModule.Config.builder().queueFilePath(QUEUE_FILE_PATH).build(),
                                () -> DEFAULT_DELEGATE_ID),
        new TailerModule(TailerModule.Config.builder()
                             .accountId(DEFAULT_ACCOUNT_ID)
                             .accountSecret(DEFAULT_ACCOUNT_SECRET)
                             .queueFilePath(QUEUE_FILE_PATH)
                             .publishTarget("localhost:" + PORT)
                             .publishAuthority("localhost")
                             .build()),
        new EventServiceModule(
            EventServiceConfig.builder().connector(new Connector(PORT, true, "cert.pem", "key.pem")).build()),
        new TestMongoModule(datastore, null));
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          applyInjector(base, method, target).evaluate();
        } finally {
          new File(QUEUE_FILE_PATH).delete();
          closingFactory.stopServers();
        }
      }
    };
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    injector.getInstance(ServiceManager.class).startAsync().awaitHealthy();
    injector.getInstance(HPersistence.class).registerUserProvider(new ThreadLocalUserProvider());
    injector.getInstance(ChronicleEventTailer.class).startAsync().awaitRunning();
  }

  @Override
  public void destroy(Injector injector, List<Module> modules) throws Exception {
    injector.getInstance(ChronicleEventTailer.class).stopAsync().awaitTerminated();
    injector.getInstance(EventPublisher.class).shutdown();
    injector.getInstance(ServiceManager.class).stopAsync().awaitStopped();
  }
}
