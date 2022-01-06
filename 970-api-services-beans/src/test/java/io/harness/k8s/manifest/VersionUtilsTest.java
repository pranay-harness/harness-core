/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.manifest;

import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.k8s.manifest.VersionUtils.addRevisionNumber;
import static io.harness.k8s.manifest.VersionUtils.markVersionedResources;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.KubernetesResource;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VersionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapAndPodEnvTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-pod-env.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, 1);

    assertThat(resourcesWithRevision.get(0).getResourceId().isVersioned()).isEqualTo(true);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-1");

    assertThat(resourcesWithRevision.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo(resources.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name") + "-1");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapsAndPodEnvTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-pod-env.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, 1);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-1");

    assertThat(resourcesWithRevision.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo(resources.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name") + "-1");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void twoConfigMapsAndPodEnvTest() throws Exception {
    URL url = this.getClass().getResource("/two-configmap-pod-env.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("metadata.name"))
        .isEqualTo(resources.get(1).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(2).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo(
            resources.get(2).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(2).getField("spec.containers[0].env[1].valueFrom.configMapKeyRef.name"))
        .isEqualTo(
            resources.get(2).getField("spec.containers[0].env[1].valueFrom.configMapKeyRef.name") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapsAndPodEnvFromTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-pod-envfrom.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("spec.containers[0].envFrom[0].configMapRef.name"))
        .isEqualTo(resources.get(1).getField("spec.containers[0].envFrom[0].configMapRef.name") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapsAndPodVolumeTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-pod-volume.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("spec.volumes[0].configMap.name"))
        .isEqualTo(resources.get(1).getField("spec.volumes[0].configMap.name") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapAndRedisPodVolumeTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-redis-pod-volume.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("spec.volumes[1].configMap.name"))
        .isEqualTo(resources.get(1).getField("spec.volumes[1].configMap.name") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void secretAndPodVolumeTest() throws Exception {
    URL url = this.getClass().getResource("/secret-pod-volume.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("spec.volumes[0].secret.secretName"))
        .isEqualTo(resources.get(1).getField("spec.volumes[0].secret.secretName") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void nginxDeploymentVersionTest() throws Exception {
    URL url = this.getClass().getResource("/nginx-full.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(1).getField("metadata.name"))
        .isEqualTo(resources.get(1).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(2).getField("metadata.name"))
        .isEqualTo(resources.get(2).getField("metadata.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(3).getField("spec.template.spec.volumes[0].configMap.name"))
        .isEqualTo(resources.get(3).getField("spec.template.spec.volumes[0].configMap.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(3).getField("spec.template.spec.volumes[1].configMap.name"))
        .isEqualTo(resources.get(3).getField("spec.template.spec.volumes[1].configMap.name") + "-" + revision);

    assertThat(resourcesWithRevision.get(3).getField("spec.template.spec.volumes[2].secret.secretName"))
        .isEqualTo(resources.get(3).getField("spec.template.spec.volumes[2].secret.secretName") + "-" + revision);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void configMapWithDirectApplyAndPodVolumeTest() throws Exception {
    URL url = this.getClass().getResource("/configmap-skip-versioning-pod-env.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    List<KubernetesResource> resources = processYaml(fileContents);

    int revision = 100;

    List<KubernetesResource> resourcesWithRevision = processYaml(fileContents);
    addRevisionNumber(resourcesWithRevision, revision);

    assertThat(resourcesWithRevision.get(0).getResourceId().isVersioned()).isEqualTo(false);

    assertThat(resourcesWithRevision.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name"));

    assertThat(resourcesWithRevision.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo(resources.get(1).getField("spec.containers[0].env[0].valueFrom.configMapKeyRef.name"));
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testMarkVersionedResources() throws IOException {
    URL url = this.getClass().getResource("/configmap-pod-env.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = processYaml(fileContents);

    List<KubernetesResource> resourcesAfterMarkedVersioned = processYaml(fileContents);
    markVersionedResources(resourcesAfterMarkedVersioned);

    assertThat(resourcesAfterMarkedVersioned.get(0).getField("metadata.name"))
        .isEqualTo(resources.get(0).getField("metadata.name"));
    assertThat(resourcesAfterMarkedVersioned.get(0).getResourceId().isVersioned()).isTrue();
    assertThat(resourcesAfterMarkedVersioned.get(1).getResourceId().isVersioned()).isFalse();
  }
}
