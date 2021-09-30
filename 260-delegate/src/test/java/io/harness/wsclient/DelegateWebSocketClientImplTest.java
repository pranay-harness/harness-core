package io.harness.wsclient;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.TokenGenerator;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(DEL)
public class DelegateWebSocketClientImplTest extends DelegateTestBase {
  DelegateWebSocketClientImpl client;

  @Before
  public void setUp() throws URISyntaxException {}

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void testOpenUnsuccessful()
      throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException, IOException {
    TokenGenerator tokenGenerator = new TokenGenerator("kmpySmUISimoRrJL6NL73w", "2f6b0988b6fb3370073c3d0505baee59");
    String token = tokenGenerator.getToken("https", "localhost", 9090, "Vladimir-Peric.local");
    URI uri = new URI(
        "wss://localhost:9090/stream/delegate/kmpySmUISimoRrJL6NL73w?delegateId=5AqFHryRQ2mR0OnHauGSJg&delegateConnectionId=owVIZhuHEeyxQdFa37lwpQ&token="
        + token + "&sequenceNum&delegateToken");
    String message =
        "{\"delegateId\":\"5AqFHryRQ2mR0OnHauGSJg\",\"accountId\":\"kmpySmUISimoRrJL6NL73w\",\"ip\":\"192.168.0.29\",\"hostName\":\"Vladimir-Peric.local\",\"delegateName\":\"\",\"delegateProfileId\":\"\",\"description\":\"\",\"version\":\"1.0.0\",\"location\":\"/private/var/tmp/_bazel_vlad/b9fa170377f574e03829125c4a4f0cdb/execroot/harness_monorepo/bazel-out/darwin-fastbuild/bin/260-delegate/execute.runfiles/harness_monorepo\",\"lastHeartBeat\":1632234289556,\"ng\":false,\"sampleDelegate\":false,\"keepAlivePacket\":false,\"pollingModeEnabled\":false,\"proxy\":false,\"ceEnabled\":false,\"currentlyExecutingDelegateTasks\":[]}";
    client = DelegateWebSocketClientImpl.builder().build();
    client.addHeader("Version", "1.0.0");
    boolean result = client.open(uri);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void testFireUnsuccessful()
      throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException, IOException {
    TokenGenerator tokenGenerator = new TokenGenerator("kmpySmUISimoRrJL6NL73w", "2f6b0988b6fb3370073c3d0505baee59");
    String token = tokenGenerator.getToken("https", "localhost", 9090, "Vladimir-Peric.local");
    URI uri = new URI(
        "wss://localhost:9090/stream/delegate/kmpySmUISimoRrJL6NL73w?delegateId=5AqFHryRQ2mR0OnHauGSJg&delegateConnectionId=owVIZhuHEeyxQdFa37lwpQ&token="
        + token + "&sequenceNum&delegateToken");
    String message =
        "{\"delegateId\":\"5AqFHryRQ2mR0OnHauGSJg\",\"accountId\":\"kmpySmUISimoRrJL6NL73w\",\"ip\":\"192.168.0.29\",\"hostName\":\"Vladimir-Peric.local\",\"delegateName\":\"\",\"delegateProfileId\":\"\",\"description\":\"\",\"version\":\"1.0.0\",\"location\":\"/private/var/tmp/_bazel_vlad/b9fa170377f574e03829125c4a4f0cdb/execroot/harness_monorepo/bazel-out/darwin-fastbuild/bin/260-delegate/execute.runfiles/harness_monorepo\",\"lastHeartBeat\":1632234289556,\"ng\":false,\"sampleDelegate\":false,\"keepAlivePacket\":false,\"pollingModeEnabled\":false,\"proxy\":false,\"ceEnabled\":false,\"currentlyExecutingDelegateTasks\":[]}";
    client = DelegateWebSocketClientImpl.builder().build();
    client.addHeader("Version", "1.0.0");
    client.open(uri);
    boolean result = client.fire(message);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void testBuildUri() {
    TokenGenerator tokenGenerator = new TokenGenerator("kmpySmUISimoRrJL6NL73w", "2f6b0988b6fb3370073c3d0505baee59");
    String token = tokenGenerator.getToken("https", "localhost", 9090, "Vladimir-Peric.local");
    assertThat(token).isNotNull();
  }
}