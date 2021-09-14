/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.network;

import static io.harness.rule.OwnerRule.BRETT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LocalhostTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetLocalHostAddress() {
    String address = Localhost.getLocalHostAddress();
    assertThat(address).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteHostname() throws InterruptedException, ExecutionException, IOException {
    String hostname = Localhost.executeHostname();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteHostnameShort() throws InterruptedException, ExecutionException, IOException {
    String hostname = Localhost.executeHostnameShort();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetLocalHostName() {
    String hostname = Localhost.getLocalHostName();
    assertThat(hostname).isNotNull();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldGetAddress() throws SocketException {
    String address = Localhost.getAddress();
    assertThat(address).isNotNull();
  }
}
