package software.wings.delegatetasks.k8s;

import static io.harness.k8s.manifest.ManifestHelper.getKubernetesResourceFromSpec;
import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.inject.Singleton;

import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Singleton
public class K8sTestHelper {
  private static String resourcePath = "./k8s";
  private static String deploymentYaml = "deployment.yaml";
  private static String deploymentConfigYaml = "deployment-config.yaml";
  private static String configMapYaml = "configMap.yaml";
  private static String serviceYaml = "service.yaml";
  private static String primaryServiceYaml = "primaryService.yaml";
  private static String stageServiceYaml = "stageService.yaml";
  private static String namespaceYaml = "namespace.yaml";

  public static KubernetesResource configMap() throws IOException {
    String yamlString = readFileContent(configMapYaml, resourcePath);
    KubernetesResource kubernetesResource = getKubernetesResourceFromSpec(yamlString);
    kubernetesResource.getResourceId().setVersioned(true);
    return kubernetesResource;
  }

  public static KubernetesResource stageService() throws IOException {
    String yamlString = readFileContent(stageServiceYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
  }

  public static KubernetesResource namespace() throws IOException {
    String yamlString = readFileContent(namespaceYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
  }

  public static KubernetesResource primaryService() throws IOException {
    String yamlString = readFileContent(primaryServiceYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
  }

  public static KubernetesResource service() throws IOException {
    String yamlString = readFileContent(serviceYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
  }

  public static KubernetesResource deployment() throws IOException {
    String yamlString = readFileContent(deploymentYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
  }

  public static KubernetesResource deploymentConfig() throws IOException {
    String yamlString = readFileContent(deploymentConfigYaml, resourcePath);
    return getKubernetesResourceFromSpec(yamlString);
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

  public static Release buildRelease(Release.Status status, int number) throws IOException {
    return Release.builder()
        .number(number)
        .resources(asList(deployment().getResourceId(), configMap().getResourceId()))
        .managedWorkload(deployment().getResourceId())
        .status(status)
        .build();
  }

  public static Release buildReleaseMultipleManagedWorkloads(Release.Status status) throws IOException {
    return Release.builder()
        .resources(asList(deployment().getResourceId(), configMap().getResourceId()))
        .managedWorkloads(asList(Release.KubernetesResourceIdRevision.builder()
                                     .workload(deploymentConfig().getResourceId())
                                     .revision("2")
                                     .build(),
            Release.KubernetesResourceIdRevision.builder()
                .workload(deployment().getResourceId())
                .revision("2")
                .build()))
        .status(status)
        .build();
  }

  public static ProcessResult buildProcessResult(int exitCode, String output) {
    return new ProcessResult(exitCode, new ProcessOutput(output.getBytes()));
  }

  public static ProcessResult buildProcessResult(int exitCode) {
    return new ProcessResult(exitCode, new ProcessOutput(null));
  }
}
