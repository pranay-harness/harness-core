/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s;

import static io.harness.rule.OwnerRule.ABOSII;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import io.kubernetes.client.util.KubeConfig;
import java.io.StringReader;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KubeConfigHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFindObject() {
    KubeConfig kubeConfig = getKubeConfig();
    Map<String, Object> result = KubeConfigHelper.findObject(kubeConfig.getClusters(), "cluster-1");
    assertThat(result).isNotNull();
    assertThat(result.containsKey("cluster")).isTrue();
    Map<String, Object> cluster = (Map<String, Object>) result.get("cluster");
    assertThat(cluster.containsKey("server")).isTrue();
    assertThat(cluster.get("server")).isEqualTo("https://master-url-1");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFindObjectEmptyList() {
    assertThat(KubeConfigHelper.findObject(emptyList(), "any")).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFindObjectMissing() {
    KubeConfig kubeConfig = getKubeConfig();

    assertThat(KubeConfigHelper.findObject(kubeConfig.getContexts(), "none")).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetCurrentUser() {
    KubeConfig kubeConfig = getKubeConfig();

    assertThat(KubeConfigHelper.getCurrentUser(kubeConfig)).isEqualTo("user-1");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetCurrentUserMissingContext() {
    KubeConfig kubeConfig = getKubeConfig();
    kubeConfig.setContext("missing");

    assertThat(KubeConfigHelper.getCurrentUser(kubeConfig)).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetCurrentUserNoCurrentUser() {
    KubeConfig kubeConfig = getKubeConfig();
    kubeConfig.setContext("context-no-user");

    assertThat(KubeConfigHelper.getCurrentUser(kubeConfig)).isNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetCurrentUserInvalidContext() {
    KubeConfig kubeConfig = getKubeConfig();
    kubeConfig.setContext("invalid-context");

    assertThat(KubeConfigHelper.getCurrentUser(kubeConfig)).isNull();
  }

  private static KubeConfig getKubeConfig() {
    return KubeConfig.loadKubeConfig(new StringReader(getKubeconfigAsString()));
  }

  private static String getKubeconfigAsString() {
    return "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    xxxxxxxx https://master-url-1\n"
        + "    certificate-authority-data: certificate-authority-data-1\n"
        + "    insecure-skip-tls-verify: true\n"
        + "  name: cluster-1\n"
        + "- cluster:\n"
        + "    xxxxxxxx https://master-url-2\n"
        + "    certificate-authority-data: certificate-authority-data-2\n"
        + "    insecure-skip-tls-verify: true\n"
        + "  name: cluster-2\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: cluster-1\n"
        + "    user: user-1\n"
        + "    namespace: namespace\n"
        + "  name: context-1\n"
        + "- context:\n"
        + "    cluster: cluster-2\n"
        + "    user: user-2\n"
        + "    namespace: namespace\n"
        + "  name: context-2\n"
        + "- context:\n"
        + "    cluster: cluster-2\n"
        + "    namespace: namespace\n"
        + "  name: context-no-user\n"
        + "- context:\n"
        + "    cluster: cluster-2\n"
        + "    namespace: namespace\n"
        + "  name: invalid-context\n"
        + "current-context: context-1\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: user-1\n"
        + "  user:\n"
        + "    client-certificate-data: client-certificate-data-1\n"
        + "    client-key-data: client-key-data-1\n"
        + "- name: user-2\n"
        + "  user:\n"
        + "    client-certificate-data: client-certificate-data-2\n"
        + "    client-key-data: client-key-data-2\n";
  }
}
