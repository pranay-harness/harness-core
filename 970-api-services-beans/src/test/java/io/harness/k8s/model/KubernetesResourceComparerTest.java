/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.model;

import static io.harness.rule.OwnerRule.PUNEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KubernetesResourceComparerTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void OrderTest1() {
    List<KubernetesResource> resources = new ArrayList<>();
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("Deployment").name("deployment1").build())
                      .build());
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("ConfigMap").name("configMap1").build())
                      .build());

    resources = resources.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());

    assertThat(resources.get(0).getResourceId().getKind()).isEqualTo("ConfigMap");
    assertThat(resources.get(1).getResourceId().getKind()).isEqualTo("Deployment");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void OrderTest2() {
    List<KubernetesResource> resources = new ArrayList<>();
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("Deployment").name("deployment1").build())
                      .build());
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("ConfigMap").name("configMap1").build())
                      .build());
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("Secret").name("secret1").build())
                      .build());

    resources = resources.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());

    assertThat(resources.get(0).getResourceId().getKind()).isEqualTo("Secret");
    assertThat(resources.get(1).getResourceId().getKind()).isEqualTo("ConfigMap");
    assertThat(resources.get(2).getResourceId().getKind()).isEqualTo("Deployment");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void OrderTest3() {
    List<KubernetesResource> resources = new ArrayList<>();
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("ConfigMap").name("configMap1").build())
                      .build());
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("ConfigMap").name("configMap2").build())
                      .build());

    resources = resources.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());

    assertThat(resources.get(0).getResourceId().getName()).isEqualTo("configMap1");
    assertThat(resources.get(1).getResourceId().getName()).isEqualTo("configMap2");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void OrderTest4() {
    List<KubernetesResource> resources = new ArrayList<>();
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("Unknown").name("unknown1").build())
                      .build());
    resources.add(KubernetesResource.builder()
                      .resourceId(KubernetesResourceId.builder().kind("ConfigMap").name("configMap1").build())
                      .build());

    resources = resources.stream().sorted(new KubernetesResourceComparer()).collect(Collectors.toList());

    assertThat(resources.get(0).getResourceId().getKind()).isEqualTo("ConfigMap");
    assertThat(resources.get(1).getResourceId().getKind()).isEqualTo("Unknown");
  }
}
