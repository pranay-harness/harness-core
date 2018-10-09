package io.harness.k8s.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class ManifestHelperTest {
  @Test
  public void toYamlSmokeTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = ManifestHelper.processYaml(fileContents);

    String serializedYaml = ManifestHelper.toYaml(resources.get(0).getValue());
    List<KubernetesResource> resources1 = ManifestHelper.processYaml(serializedYaml);

    assertThat(resources1.get(0).getResourceId()).isEqualTo(resources.get(0).getResourceId());
    assertThat(resources1.get(0).getValue()).isEqualTo(resources.get(0).getValue());
  }

  @Test
  public void processYamlSmokeTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = ManifestHelper.processYaml(fileContents);

    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getResourceId())
        .isEqualTo(KubernetesResourceId.builder().kind("Deployment").name("nginx-deployment").build());
  }

  @Test
  public void processYamlMultiResourceTest() throws Exception {
    URL url = this.getClass().getResource("/mongo.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = ManifestHelper.processYaml(fileContents);

    assertThat(resources).hasSize(4);
    assertThat(resources.get(0).getResourceId())
        .isEqualTo(KubernetesResourceId.builder().kind("Namespace").name("mongo").build());

    assertThat(resources.get(1).getResourceId())
        .isEqualTo(
            KubernetesResourceId.builder().kind("PersistentVolumeClaim").name("mongo-data").namespace("mongo").build());

    assertThat(resources.get(2).getResourceId())
        .isEqualTo(KubernetesResourceId.builder().kind("Service").name("mongo").namespace("mongo").build());

    assertThat(resources.get(3).getResourceId())
        .isEqualTo(KubernetesResourceId.builder().kind("Deployment").name("mongo").namespace("mongo").build());
  }

  @Test
  public void processYamlMissingKindTest() throws Exception {
    URL url = this.getClass().getResource("/missing-kind.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      ManifestHelper.processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Error processing yaml manifest. kind not found in spec.");
    }
  }

  @Test
  public void processYamlMissingNameTest() throws Exception {
    URL url = this.getClass().getResource("/missing-name.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      ManifestHelper.processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Error processing yaml manifest. metadata.name not found in spec.");
    }
  }
}
