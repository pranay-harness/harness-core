package software.wings.delegatetasks.k8s;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.inject.Singleton;

import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Singleton
public class K8sTestHelper {
  private static String resourcePath = "./k8s";
  private static String deploymentYaml = "deployment.yaml";
  private static String deploymentConfigYaml = "deployment-config.yaml";
  private static String configMapYaml = "configMap.yaml";

  public static KubernetesResource configMap() throws IOException {
    String yamlString = readFileContent(configMapYaml, resourcePath);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(KubernetesResourceId.builder()
                        .namespace("default")
                        .kind("configMap")
                        .name("config-map")
                        .versioned(true)
                        .build())
        .build();
  }

  public static KubernetesResource deployment() throws IOException {
    String yamlString = readFileContent(deploymentYaml, resourcePath);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(
            KubernetesResourceId.builder().namespace("default").kind("Deployment").name("nginx-deployment").build())
        .build();
  }

  public static KubernetesResource deploymentConfig() throws IOException {
    String yamlString = readFileContent(deploymentConfigYaml, resourcePath);
    return KubernetesResource.builder()
        .spec(yamlString)
        .resourceId(
            KubernetesResourceId.builder().namespace("default").kind("DeploymentConfig").name("test-dc").build())
        .build();
  }

  public static String readFileContent(String filePath, String resourcePath) throws IOException {
    File yamlFile = null;
    try {
      yamlFile =
          new File(K8sTestHelper.class.getClassLoader().getResource(resourcePath + PATH_DELIMITER + filePath).toURI());
    } catch (URISyntaxException e) {
      Assertions.fail("Unable to find yaml file " + filePath);
    }
    return FileUtils.readFileToString(yamlFile, "UTF-8");
  }
}
