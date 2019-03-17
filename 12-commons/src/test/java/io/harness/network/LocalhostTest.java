package io.harness.network;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;

public class LocalhostTest {
  @Test
  @Category(UnitTests.class)
  public void shouldGetLocalHostAddress() {
    String address = Localhost.getLocalHostAddress();
    assertThat(address).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteHostname() throws InterruptedException, ExecutionException, IOException {
    String hostname = Localhost.executeHostname();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecuteHostnameShort() throws InterruptedException, ExecutionException, IOException {
    String hostname = Localhost.executeHostnameShort();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetLocalHostName() {
    String hostname = Localhost.getLocalHostName();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAddress() throws SocketException {
    String address = Localhost.getAddress();
    assertThat(address).isNotNull();
  }
}
