/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.PUNEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testAllResources() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("all");

    assertThat(getCommand.command()).isEqualTo("kubectl get all");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testAllPodsInNamespace() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources(ResourceType.pods.toString()).namespace("default").output("yaml");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods --namespace=default --output=yaml");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testSpecificPod() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods/web-0");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods/web-0");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testAllPodsAndServices() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods,services");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods,services");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testGetEvents() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("events").namespace("default").watchOnly(true);

    assertThat(getCommand.command()).isEqualTo("kubectl get events --namespace=default --watch-only");
  }
}
