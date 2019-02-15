package io.harness.k8s.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import io.harness.k8s.manifest.ManifestHelper;
import org.junit.Test;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class KubernetesResourceTest {
  @Test
  public void setAndGetTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = ManifestHelper.processYaml(fileContents).get(0);

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
  public void arrayFieldsSetAndGetTest() throws Exception {
    URL url = this.getClass().getResource("/two-containers.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = ManifestHelper.processYaml(fileContents).get(0);

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
  public void addAnnotationTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = ManifestHelper.processYaml(fileContents).get(0);

    resource.addAnnotations(ImmutableMap.of("key1", "value1", "key2", "value2"));

    Map annotations = (Map) resource.getField("metadata.annotations");

    assertThat(annotations).hasSize(2);
    assertThat(annotations.get("key1")).isEqualTo("value1");
    assertThat(annotations.get("key2")).isEqualTo("value2");
  }

  @Test
  public void addLabelsTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = ManifestHelper.processYaml(fileContents).get(0);

    resource.addLabels(ImmutableMap.of("key1", "value1", "key2", "value2"));

    Map labels = (Map) resource.getField("metadata.labels");

    assertThat(labels).hasSize(3);
    assertThat(labels.get("app")).isEqualTo("nginx");
    assertThat(labels.get("key1")).isEqualTo("value1");
    assertThat(labels.get("key2")).isEqualTo("value2");
  }

  @Test
  public void nameUpdateTests() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    KubernetesResource resource = ManifestHelper.processYaml(fileContents).get(0);
    UnaryOperator<Object> appendRevision = t -> t + "-1";

    String oldName = (String) resource.getField("metadata.name");

    resource.transformName(appendRevision);

    assertThat(resource.getField("metadata.name")).isEqualTo(oldName + "-1");
  }
}
