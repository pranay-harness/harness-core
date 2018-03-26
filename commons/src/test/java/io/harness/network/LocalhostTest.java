package io.harness.network;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.net.SocketException;

public class LocalhostTest {
  @Test
  public void shouldGetLocalHostAddress() {
    String address = Localhost.getLocalHostAddress();
    assertThat(address).isNotNull();
  }

  @Test
  public void shouldExecuteHostname() {
    try {
      String hostname = Localhost.executeHostname();
      assertThat(hostname).isNotNull();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void shouldGetLocalHostName() {
    String hostname = Localhost.getLocalHostName();
    assertThat(hostname).isNotNull();
  }

  @Test
  public void shouldGetAddress() {
    try {
      String address = Localhost.getAddress();
      assertThat(address).isNotNull();
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }
}
