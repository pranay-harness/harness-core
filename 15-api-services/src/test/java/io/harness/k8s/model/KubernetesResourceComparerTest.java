package io.harness.k8s.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KubernetesResourceComparerTest {
  @Test
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
