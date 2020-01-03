package io.harness.k8s.manifest;

import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static io.harness.k8s.manifest.ManifestHelper.MAX_VALUES_EXPRESSION_RECURSION_DEPTH;
import static io.harness.k8s.manifest.ManifestHelper.getMapFromValuesFileContent;
import static io.harness.k8s.manifest.ManifestHelper.getValuesExpressionKeysFromMap;
import static io.harness.k8s.manifest.ManifestHelper.processYaml;
import static io.harness.k8s.manifest.ManifestHelper.validateValuesFileContents;
import static io.harness.k8s.manifest.ObjectYamlUtils.toYaml;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.PUNEET;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.ExceptionLogger;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ManifestHelperTest extends CategoryTest {
  @Before
  public void setup() {
    initializeLogging();
  }

  @Test
  @Owner(developers = PUNEET)
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
  @Owner(developers = PUNEET)
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
  @Owner(developers = PUNEET)
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
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void invalidYamlTest() {
    try {
      processYaml(":");
    } catch (KubernetesYamlException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly("Invalid Kubernetes YAML Spec. Error parsing YAML..");
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void invalidYamlObjectTest() {
    try {
      processYaml("object");
    } catch (KubernetesYamlException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly("Invalid Kubernetes YAML Spec. Invalid Yaml. Object is not a map..");
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void processYamlMissingKindTest() throws Exception {
    URL url = this.getClass().getResource("/missing-kind.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly("Invalid Kubernetes YAML Spec. Error processing yaml manifest. kind not found in spec..");
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void processYamlMissingNameTest() throws Exception {
    URL url = this.getClass().getResource("/missing-name.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    try {
      processYaml(fileContents);
    } catch (KubernetesYamlException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly(
              "Invalid Kubernetes YAML Spec. Error processing yaml manifest. metadata.name not found in spec..");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNormalizeFolderPath() {
    assertThat(ManifestHelper.normalizeFolderPath("abc")).isEqualTo("abc/");
    assertThat(ManifestHelper.normalizeFolderPath("abc/")).isEqualTo("abc/");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetMapFromValuesFileContent() throws Exception {
    URL url = this.getClass().getResource("/sample-values.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);

    Map map = getMapFromValuesFileContent(fileContents);
    assertThat(map.size()).isEqualTo(2);
    assertThat(map.containsKey("Master")).isTrue();

    Map masterMap = (Map) map.get("Master");
    assertThat(masterMap.size()).isEqualTo(5);
    assertThat(masterMap.containsKey("Name")).isTrue();
    assertThat(masterMap.containsKey("resources")).isTrue();

    Map resourcesMap = (Map) masterMap.get("resources");
    assertThat(resourcesMap.containsKey("requests")).isTrue();

    assertThat(map.containsKey("Slave")).isTrue();
    Map slaveMap = (Map) map.get("Slave");
    assertThat(slaveMap.containsKey("InstallPlugins")).isTrue();
  }

  @Test
  @Owner(developers = ANSHUL)
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
  @Owner(developers = ANSHUL)
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
    assertThat(expressionSet.containsAll(expectedExpressionSet)).isTrue();
  }

  @Test
  @Owner(developers = ANSHUL)
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

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testValidateValuesFileContentGoodCases() {
    validateValuesFileContents("");
    validateValuesFileContents("# empty file");
    validateValuesFileContents("name: account-service\n"
        + "replicas: 1");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testValidateValuesFileContentNotAMap() {
    try {
      validateValuesFileContents("test");
      fail("Invalid values content not caught.");
    } catch (WingsException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly("Invalid values file. Object is not a map..");
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testValidateValuesFileContentInvalidStructure() {
    try {
      validateValuesFileContents("name: account-service\n"
          + "replicas ");
      fail("Invalid values content not caught.");
    } catch (WingsException e) {
      assertThat(ExceptionLogger.getResponseMessageList(e, LOG_SYSTEM))
          .extracting(ResponseMessage::getMessage)
          .containsExactly("Invalid values file. Error parsing YAML. Line 1, column 9: "
              + "Expected a 'block end' but found: scalar. @[<>].");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetWorkloadsForCanary() {
    KubernetesResource deployment = KubernetesResource.builder()
                                        .resourceId(KubernetesResourceId.builder().kind(Kind.Deployment.name()).build())
                                        .build();

    List<KubernetesResource> kubernetesResources = ManifestHelper.getWorkloadsForCanary(asList(deployment));
    assertThat(kubernetesResources.size()).isEqualTo(1);
    assertThat(kubernetesResources.get(0)).isEqualTo(deployment);

    KubernetesResource statefulSet =
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().kind(Kind.StatefulSet.name()).build())
            .build();

    kubernetesResources = ManifestHelper.getWorkloadsForCanary(asList(deployment, statefulSet));
    assertThat(kubernetesResources.size()).isEqualTo(1);
    assertThat(kubernetesResources.get(0)).isEqualTo(deployment);

    KubernetesResource daemonSet = KubernetesResource.builder()
                                       .resourceId(KubernetesResourceId.builder().kind(Kind.DaemonSet.name()).build())
                                       .build();

    kubernetesResources = ManifestHelper.getWorkloadsForCanary(asList(deployment, statefulSet, daemonSet));
    assertThat(kubernetesResources.size()).isEqualTo(1);
    assertThat(kubernetesResources.get(0)).isEqualTo(deployment);

    List<KubernetesResource> deploymentDirectApply = ManifestHelper.processYaml("apiVersion: apps/v1\n"
        + "kind: Deployment\n"
        + "metadata:\n"
        + "  name: deployment\n"
        + "  annotations:\n"
        + "    harness.io/direct-apply: true\n"
        + "spec:\n"
        + "  replicas: 1");

    kubernetesResources =
        ManifestHelper.getWorkloadsForCanary(asList(deployment, statefulSet, daemonSet, deploymentDirectApply.get(0)));
    assertThat(kubernetesResources.size()).isEqualTo(1);
    assertThat(kubernetesResources.get(0)).isEqualTo(deployment);
  }
}
