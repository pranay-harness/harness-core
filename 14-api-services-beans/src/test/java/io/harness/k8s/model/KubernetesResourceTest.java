package io.harness.k8s.model;

import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.SATYAM;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class KubernetesResourceTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void setAndGetTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    assertThat(resource.getField("random")).isEqualTo(null);
    assertThat(resource.getField("random.random")).isEqualTo(null);

    resource.setField("kind", "myKind");
    String kind = (String) resource.getField("kind");
    assertThat(kind).isEqualTo("myKind");

    resource.setField("metadata.name", "myName");
    String name = (String) resource.getField("metadata.name");
    assertThat(name).isEqualTo("myName");

    resource.setField("metadata.labels.key1", "value1");
    String key = (String) resource.getField("metadata.labels.key1");
    assertThat(key).isEqualTo("value1");

    resource.setField("metadata.labels.key2", "value2");
    Map labels = (Map) resource.getField("metadata.labels");
    assertThat(labels).hasSize(3);
    assertThat(labels.get("app")).isEqualTo("nginx");
    assertThat(labels.get("key1")).isEqualTo("value1");
    assertThat(labels.get("key2")).isEqualTo("value2");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void arrayFieldsSetAndGetTest() throws Exception {
    URL url = this.getClass().getResource("/two-containers.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    String containerName = (String) resource.getField("spec.containers[0].name");
    assertThat(containerName).isEqualTo("nginx-container");

    containerName = (String) resource.getField("spec.containers[1].name");
    assertThat(containerName).isEqualTo("debian-container");

    Object obj = resource.getField("spec.containers[0]");
    assertThat(obj).isInstanceOf(Map.class);

    obj = resource.getFields("spec.containers[]");
    assertThat(obj).isInstanceOf(List.class);

    resource.setField("spec.containers[0].name", "hello");
    containerName = (String) resource.getField("spec.containers[0].name");
    assertThat(containerName).isEqualTo("hello");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void addAnnotationTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    resource.addAnnotations(ImmutableMap.of("key1", "value1", "key2", "value2"));

    Map annotations = (Map) resource.getField("metadata.annotations");

    assertThat(annotations).hasSize(2);
    assertThat(annotations.get("key1")).isEqualTo("value1");
    assertThat(annotations.get("key2")).isEqualTo("value2");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void addLabelsTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    resource.addLabels(ImmutableMap.of("key1", "value1", "key2", "value2"));

    Map labels = (Map) resource.getField("metadata.labels");

    assertThat(labels).hasSize(3);
    assertThat(labels.get("app")).isEqualTo("nginx");
    assertThat(labels.get("key1")).isEqualTo("value1");
    assertThat(labels.get("key2")).isEqualTo("value2");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void nameUpdateTests() throws Exception {
    nameUpdateTestsUtil("deploy.yaml", true);
    nameUpdateTestsUtil("service.yaml", true);
    nameUpdateTestsUtil("configmap.yaml", true);
    nameUpdateTestsUtil("secret.yaml", true);
    nameUpdateTestsUtil("daemonset.yaml", false);
  }

  private void nameUpdateTestsUtil(String resourceFile, boolean shouldNameChange) throws Exception {
    URL url = this.getClass().getResource("/" + resourceFile);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    UnaryOperator<Object> appendRevision = t -> t + "-1";

    String oldName = (String) resource.getField("metadata.name");

    resource.transformName(appendRevision);

    if (shouldNameChange) {
      assertThat(resource.getField("metadata.name")).isEqualTo(oldName + "-1");
    } else {
      assertThat(resource.getField("metadata.name")).isEqualTo(oldName);
    }
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testAddLabelsInPodSpecNullPodTemplateSpec() throws Exception {
    URL url = this.getClass().getResource("/null-pod-template.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.addLabelsInPodSpec(ImmutableMap.of("k", "v"));
    assertThat(resource).isNotNull();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testTransformConfigMapAndSecretRef() throws Exception {
    URL url = this.getClass().getResource("/spec-in-template-null.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.transformConfigMapAndSecretRef(UnaryOperator.identity(), UnaryOperator.identity());
    assertThat(resource).isNotNull();
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testGetSpecForStatefulSet() throws Exception {
    URL url1 = this.getClass().getResource("/denormalized-stateful-set-spec.yaml");
    String denormalizedSpec = IOUtils.toString(url1, Charsets.UTF_8);

    URL url2 = this.getClass().getResource("/normalized-stateful-set-spec.yaml");
    String normalizedSpec = IOUtils.toString(url2, Charsets.UTF_8);

    KubernetesResource denormalizedResource = processYaml(denormalizedSpec).get(0);
    assertEquals(normalizedSpec, denormalizedResource.getSpec());

    KubernetesResource normalizedResource = processYaml(normalizedSpec).get(0);
    assertEquals(normalizedSpec, normalizedResource.getSpec());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testGetSpecForNonStatefulSet() throws Exception {
    URL url1 = this.getClass().getResource("/deploy.yaml");
    String spec = IOUtils.toString(url1, Charsets.UTF_8);

    KubernetesResource denormalizedResource = processYaml(spec).get(0);
    assertEquals(spec.trim(), denormalizedResource.getSpec().trim());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddLabelsInPodSpec() throws Exception {
    addLabelsInPodSpecUtil("job.yaml", true);
    addLabelsInPodSpecUtil("daemonset.yaml", true);
    addLabelsInPodSpecUtil("statefulset.yaml", true);
    addLabelsInPodSpecUtil("secret.yaml", false);
  }

  private void addLabelsInPodSpecUtil(String resourceFile, boolean verifyLabels) throws Exception {
    URL url = this.getClass().getResource("/" + resourceFile);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.addLabelsInPodSpec(ImmutableMap.of("key", "val"));
    assertThat(resource).isNotNull();
    if (verifyLabels) {
      assertThat(resource.getField("spec.template.metadata.labels.key")).isEqualTo("val");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetV1PodSpec() throws Exception {
    testGetV1PodSpecUtil("job.yaml");
    testGetV1PodSpecUtil("daemonset.yaml");
    testGetV1PodSpecUtil("statefulset.yaml");

    URL url = this.getClass().getResource("/pod.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.transformConfigMapAndSecretRef(UnaryOperator.identity(), UnaryOperator.identity());
    assertThat(resource.getField("spec.containers")).isNotNull();

    url = this.getClass().getResource("/secret.yaml");
    fileContents = Resources.toString(url, Charsets.UTF_8);
    resource = processYaml(fileContents).get(0);
    resource = resource.transformConfigMapAndSecretRef(UnaryOperator.identity(), UnaryOperator.identity());
    assertThat(resource).isNotNull();
  }

  private void testGetV1PodSpecUtil(String resourceFile) throws Exception {
    URL url = this.getClass().getResource("/" + resourceFile);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.transformConfigMapAndSecretRef(UnaryOperator.identity(), UnaryOperator.identity());
    assertThat(resource.getField("spec.template.spec.containers")).isNotNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testIsLoadBalancerService() throws Exception {
    URL url = this.getClass().getResource("/pod.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    assertThat(resource.isLoadBalancerService()).isFalse();

    url = this.getClass().getResource("/loadbalancer_service.yaml");
    fileContents = Resources.toString(url, Charsets.UTF_8);
    resource = processYaml(fileContents).get(0);
    assertThat(resource.isLoadBalancerService()).isTrue();

    url = this.getClass().getResource("/service.yaml");
    fileContents = Resources.toString(url, Charsets.UTF_8);
    resource = processYaml(fileContents).get(0);
    assertThat(resource.isLoadBalancerService()).isFalse();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddColorSelectorInService() throws Exception {
    URL url = this.getClass().getResource("/loadbalancer_service.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.addColorSelectorInService("blue");
    Map<String, String> selectors = (Map<String, String>) resource.getField("spec.selector");
    assertThat(selectors).containsKey("harness.io/color");
    assertThat(selectors.get("harness.io/color")).isEqualTo("blue");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetReplicaCount() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    assertThat(resource.getReplicaCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSetReplicaCount() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.setReplicaCount(5);
    assertThat(resource.getReplicaCount()).isEqualTo(5);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddLabelsInDeploymentSelector() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = resource.addLabelsInDeploymentSelector(ImmutableMap.of("key", "val"));
    assertThat(resource.getField("spec.selector.matchLabels.key")).isEqualTo("val");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRedactSecretValues() throws Exception {
    URL url = this.getClass().getResource("/secret.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);
    resource = processYaml(resource.redactSecretValues(resource.getSpec())).get(0);
    assertThat(resource.getField("stringData.cred")).isEqualTo("***");
    assertThat(resource.getField("data.username")).isEqualTo("***");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateConfigMapAndSecretRef() throws Exception {
    URL url = this.getClass().getResource("/deployment-envfrom.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = processYaml(fileContents).get(0);

    UnaryOperator<Object> configMapRevision = t -> t + "-1";
    UnaryOperator<Object> secretRevision = t -> t + "-2";
    resource = resource.transformConfigMapAndSecretRef(configMapRevision, secretRevision);
    assertThat(resource).isNotNull();
    assertThat(resource.getField("spec.template.spec.containers[0].envFrom.configMapRef[0].configMapRef.name"))
        .isEqualTo("example-1");
    assertThat(resource.getField("spec.template.spec.containers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo("myconfig-1");
    assertThat(resource.getField("spec.template.spec.initContainers[0].envFrom.configMapRef[0].configMapRef.name"))
        .isEqualTo("example-1");
    assertThat(resource.getField("spec.template.spec.initContainers[0].env[0].valueFrom.configMapKeyRef.name"))
        .isEqualTo("myconfig-1");

    assertThat(resource.getField("spec.template.spec.volumes[0].configMap.name")).isEqualTo("volume-config-1");
    assertThat(resource.getField("spec.template.spec.volumes[1].projected.sources[0].configMap.name"))
        .isEqualTo("configmap-projection-1");

    assertThat(resource.getField("spec.template.spec.containers[0].envFrom[1].secretRef.name")).isEqualTo("example-2");
    assertThat(resource.getField("spec.template.spec.containers[0].env[1].valueFrom.secretKeyRef.name"))
        .isEqualTo("mysecret-2");
    assertThat(resource.getField("spec.template.spec.initContainers[0].envFrom[1].secretRef.name"))
        .isEqualTo("example-2");
    assertThat(resource.getField("spec.template.spec.initContainers[0].env[1].valueFrom.secretKeyRef.name"))
        .isEqualTo("mysecret-2");
    assertThat(resource.getField("spec.template.spec.volumes[0].secret.secretName")).isEqualTo("volume-secret-2");
    assertThat(resource.getField("spec.template.spec.volumes[1].projected.sources[1].secret.name"))
        .isEqualTo("secret-projection-2");
    assertThat(resource.getField("spec.template.spec.imagePullSecrets[0].name")).isEqualTo("image_pull_secret-2");
  }
}
