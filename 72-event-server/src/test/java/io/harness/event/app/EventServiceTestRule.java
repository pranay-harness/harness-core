package io.harness.event.app;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import io.harness.event.client.EventPublisher;
import io.harness.event.client.PublisherModule;
import io.harness.event.client.PublisherModule.Config;
import io.harness.factory.ClosingFactory;
import io.harness.grpc.auth.AuthService;
import io.harness.grpc.auth.EventServiceTokenGenerator;
import io.harness.grpc.server.Connector;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.rule.InjectorRuleMixin;
import io.harness.rule.MongoRuleMixin;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class EventServiceTestRule implements MethodRule, MongoRuleMixin, InjectorRuleMixin {
  static final String DEFAULT_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
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
    return ImmutableList.of(
        Modules
            .override(new PublisherModule(Config.builder()
                                              .publishTarget("localhost:" + PORT)
                                              .publishAuthority("localhost")
                                              .queueFilePath(QUEUE_FILE_PATH)
                                              .accountId(DEFAULT_ACCOUNT_ID)
                                              .build()),
                new EventServiceModule(
                    EventServiceConfig.builder().connector(new Connector(PORT, true, "cert.pem", "key.pem")).build()),
                new TestMongoModule(datastore, null))

            // TODO(avmohan): Remove this once [CCM-47] is done.
            .with(new AbstractModule() {
              @Override
              protected void configure() {
                bind(EventServiceTokenGenerator.class).toInstance(() -> "dummy");
                bind(AuthService.class).toInstance(token -> {});
              }
            }));
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
  }

  @Override
  public void destroy(Injector injector, List<Module> modules) throws Exception {
    injector.getInstance(EventPublisher.class).shutdown();
    injector.getInstance(ServiceManager.class).stopAsync().awaitStopped();
  }
}
