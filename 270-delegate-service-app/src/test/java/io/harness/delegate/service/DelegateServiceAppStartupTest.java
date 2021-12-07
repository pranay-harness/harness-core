package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.XIN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.app.DelegateServiceApplication;
import io.harness.delegate.app.DelegateServiceConfig;
import io.harness.network.Http;
import io.harness.resource.Project;
import io.harness.rule.Owner;

import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateServiceAppStartupTest extends DelegateServiceAppTestBase {
  public static MongoServer MONGO_SERVER;
  public static DropwizardTestSupport<DelegateServiceConfig> SUPPORT;

  private static MongoServer startMongoServer() {
    final MongoServer mongoServer = new MongoServer(new MemoryBackend());
    mongoServer.bind("localhost", 0);
    return mongoServer;
  }

  private static void stopMongoServer() {
    if (MONGO_SERVER != null) {
      MONGO_SERVER.shutdownNow();
    }
  }

  private static String getMongoUri() {
    InetSocketAddress serverAddress = MONGO_SERVER.getLocalAddress();
    final ServerAddress addr = new ServerAddress(serverAddress);
    return String.format("mongodb://%s:%s/ng-harness", addr.getHost(), addr.getPort());
  }

  @BeforeClass
  public static void beforeClass() {
    MONGO_SERVER = startMongoServer();
    String directoryPath = Project.moduleDirectory(DelegateServiceAppStartupTest.class);
    String configPath = Paths.get(directoryPath, "delegate-service-config.yml").toString();
    SUPPORT = new DropwizardTestSupport<DelegateServiceConfig>(DelegateServiceApplication.class,
        String.valueOf(new File(configPath)), ConfigOverride.config("server.applicationConnectors[0].port", "0"),
        ConfigOverride.config("server.applicationConnectors[0].type", "https"),
        ConfigOverride.config("server.adminConnectors[0].type", "https"),
        ConfigOverride.config("server.adminConnectors[0].port", "0"),
        ConfigOverride.config("eventsFramework.redis.redisUrl", "dummyRedisUrl"),
        ConfigOverride.config("mongo.uri", getMongoUri()));
    SUPPORT.before();
  }

  @AfterClass
  public static void afterClass() {
    SUPPORT.after();
    stopMongoServer();
  }

  @Test
  @Owner(developers = XIN)
  @Category(UnitTests.class)
  public void testAppStartup() {
    final Client client = new JerseyClientBuilder().sslContext(Http.getSslContext()).build();
    final Response response =
        client.target(String.format("https://localhost:%d/api/swagger.json", SUPPORT.getLocalPort())).request().get();
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    response.close();
  }
}
