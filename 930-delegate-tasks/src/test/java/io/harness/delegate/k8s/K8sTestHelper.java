package io.harness.delegate.k8s;

import static io.harness.k8s.manifest.ManifestHelper.getKubernetesResourceFromSpec;

import static java.util.Arrays.asList;

import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

public class K8sTestHelper {
  private static final String BASE_PATH = "k8s";
  private static final String CONFIG_MAP = "configMap.yaml";
  private static final String DEPLOYMENT_CONFIG = "deployment-config.yaml";
  private static final String DEPLOYMENT = "deployment.yaml";
  private static final String SERVICE = "service.yaml";
  private static final String CRD_OLD = "crd-old.yaml";
  private static final String CRD_NEW = "crd-new.yaml";

  public static KubernetesResource configMap() throws IOException {
    String yamlFileContent = readResourceFileContent(CONFIG_MAP);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static KubernetesResource deploymentConfig() throws IOException {
    String yamlFileContent = readResourceFileContent(DEPLOYMENT_CONFIG);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static KubernetesResource deployment() throws IOException {
    String yamlFileContent = readResourceFileContent(DEPLOYMENT);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static KubernetesResource crdOld() throws IOException {
    String yamlFileContent = readResourceFileContent(CRD_OLD);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static KubernetesResource crdNew() throws IOException {
    String yamlFileContent = readResourceFileContent(CRD_NEW);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static KubernetesResource service() throws IOException {
    String yamlFileContent = readResourceFileContent(SERVICE);
    return getKubernetesResourceFromSpec(yamlFileContent);
  }

  public static String readResourceFileContent(String resourceFilePath) throws IOException {
    ClassLoader classLoader = K8sTestHelper.class.getClassLoader();
    return Resources.toString(
        Objects.requireNonNull(classLoader.getResource(BASE_PATH + "/" + resourceFilePath)), StandardCharsets.UTF_8);
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
