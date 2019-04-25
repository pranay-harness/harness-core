package io.harness.k8s.manifest;

import static io.harness.k8s.manifest.ManifestHelper.MAX_VALUES_EXPRESSION_RECURSION_DEPTH;
import static io.harness.k8s.manifest.ManifestHelper.getMapFromValuesFileContent;
import static io.harness.k8s.manifest.ManifestHelper.getValuesExpressionKeysFromMap;
import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.k8s.manifest.ObjectYamlUtils.toYaml;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.category.element.UnitTests;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManifestHelperTest {
  @Test
  @Category(UnitTests.class)
  public void toYamlSmokeTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = processYaml(fileContents);

    String serializedYaml = toYaml(resources.get(0).getValue());
    List<KubernetesResource> resources1 = processYaml(serializedYaml);

    assertThat(resources1.get(0).getResourceId()).isEqualTo(resources.get(0).getResourceId());
    assertThat(resources1.get(0).getValue()).isEqualTo(resources.get(0).getValue());
  }

  @Test
  @Category(UnitTests.class)
  public void processYamlSmokeTest() throws Exception {
    URL url = this.getClass().getResource("/deploy.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = processYaml(fileContents);

    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getResourceId())
        .isEqualTo(KubernetesResourceId.builder().kind("Deployment").name("nginx-deployment").build());
  }

  @Test
  @Category(UnitTests.class)
  public void processYamlMultiResourceTest() throws Exception {
    URL url = this.getClass().getResource("/mongo.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    List<KubernetesResource> resources = processYaml(fileContents);

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
  @Category(UnitTests.class)
  public void invalidYamlTest() {
    try {
      processYaml(":");
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Error parsing YAML.");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void invalidYamlObjectTest() {
    try {
      processYaml("object");
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid Yaml. Object is not a map.");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void processYamlMissingKindTest() throws Exception {
    URL url = this.getClass().getResource("/missing-kind.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Error processing yaml manifest. kind not found in spec.");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void processYamlMissingNameTest() throws Exception {
    URL url = this.getClass().getResource("/missing-name.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(e.getMessage()).isEqualTo("Error processing yaml manifest. metadata.name not found in spec.");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testNormalizeFolderPath() {
    assertThat(ManifestHelper.normalizeFolderPath("abc")).isEqualTo("abc/");
    assertThat(ManifestHelper.normalizeFolderPath("abc/")).isEqualTo("abc/");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMapFromValuesFileContent() throws Exception {
    URL url = this.getClass().getResource("/sample-values.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    Map map = getMapFromValuesFileContent(fileContents);
    assertThat(map.size()).isEqualTo(2);
    assertTrue(map.containsKey("Master"));

    Map masterMap = (Map) map.get("Master");
    assertThat(masterMap.size()).isEqualTo(5);
    assertTrue(masterMap.containsKey("Name"));
    assertTrue(masterMap.containsKey("resources"));

    Map resourcesMap = (Map) masterMap.get("resources");
    assertTrue(resourcesMap.containsKey("requests"));

    assertTrue(map.containsKey("Slave"));
    Map slaveMap = (Map) map.get("Slave");
    assertTrue(slaveMap.containsKey("InstallPlugins"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMapFromValuesFileContentWithInvalidYaml() {
    try {
      getMapFromValuesFileContent("hello: world \n"
          + "manifest \n"
          + "fruits: veggies");
    } catch (WingsException e) {
      assertThat(e.getMessage())
          .isEqualTo("Error parsing YAML. Line 2, column 6: Expected a 'block end' but found: scalar. @[fruits]");
      return;
    }

    assert false;
  }

  @Test
  @Category(UnitTests.class)
  public void testGetValuesExpressionKeysFromMap() throws Exception {
    URL url = this.getClass().getResource("/sample-values.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    Map map = getMapFromValuesFileContent(fileContents);
    Set<String> expressionSet = getValuesExpressionKeysFromMap(map, "", 0);

    Set<String> expectedExpressionSet = new HashSet<>(asList(".Values.Slave", ".Values.Master",
        ".Values.Slave.InstallPlugins", ".Values.Master.RollingUpdate", ".Values.Master.resources",
        ".Values.Master.resources.requests.memory", ".Values.Master.resources.requests", ".Values.Master.Name",
        ".Values.Master.lifecycle", ".Values.Master.resources.requests.cpu", ".Values.Master.customInitContainers"));

    assertThat(expressionSet.size()).isEqualTo(expectedExpressionSet.size());
    assertTrue(expressionSet.containsAll(expectedExpressionSet));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetValuesExpressionKeysFromMapWithDepthLimit() {
    Map map = getMapFromValuesFileContent("A:\n"
        + "  B:\n"
        + "    C:\n"
        + "      D:\n"
        + "        E:\n"
        + "          F:\n"
        + "            G:\n"
        + "              H:\n"
        + "                I:\n"
        + "                  J:\n"
        + "                    K:\n"
        + "                      L:\n"
        + "                        M:\n"
        + "                          N: \"n\"\n"
        + "                          P: \"p\"\n"
        + "                          Q: \"q\"");
    try {
      getValuesExpressionKeysFromMap(map, "", 0);
    } catch (WingsException e) {
      assertThat(e.getMessage())
          .isEqualTo("Map is too deep. Max levels supported are " + MAX_VALUES_EXPRESSION_RECURSION_DEPTH);
      return;
    }

    assert false;
  }
}
