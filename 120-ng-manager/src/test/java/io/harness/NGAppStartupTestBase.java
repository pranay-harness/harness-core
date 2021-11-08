package io.harness;

import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.NextGenApplication;
import io.harness.ng.NextGenConfiguration;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.rule.Owner;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.InetSocketAddress;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

@OwnedBy(HarnessTeam.CDC)
// @RunWith(PowerMockRunner.class) // TODO
@PrepareForTest({PmsSdkModule.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*", "javax.management.*"})
public class NGAppStartupTestBase extends CategoryTest {
  public static MongoServer MONGO_SERVER;
  public static DropwizardTestSupport<NextGenConfiguration> SUPPORT;

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

  //  @BeforeClass
  public static void beforeClass() throws Exception {
    MONGO_SERVER = startMongoServer();
    //        initializeDefaultInstance(any());
    SUPPORT = new DropwizardTestSupport<NextGenConfiguration>(NextGenApplication.class,
        String.valueOf(new File("120-ng-manager/src/test/resources/test-config.yml")),
        ConfigOverride.config("mongo.uri", getMongoUri()));
    SUPPORT.before();
  }

  //  @AfterClass
  public static void afterClass() {
    SUPPORT.after();
    stopMongoServer();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  @Ignore("Get covered by contract testing")
  public void testAppStartup() {
    final Client client = new JerseyClientBuilder().build();
    final Response response =
        client.target(String.format("http://localhost:%d/health", SUPPORT.getLocalPort())).request().get();

    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    response.close();
  }
}
