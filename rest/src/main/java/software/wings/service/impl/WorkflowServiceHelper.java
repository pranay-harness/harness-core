package software.wings.service.impl;

import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import software.wings.exception.WingsException;

import java.io.IOException;

@Singleton
public class WorkflowServiceHelper {
  public static final String MIN_REPLICAS = "\\$\\{MIN_REPLICAS}";
  public static final String MAX_REPLICAS = "\\$\\{MAX_REPLICAS}";
  public static final String UTILIZATION = "\\$\\{UTILIZATION}";
  // yaml template for custom metric HPA for cup utilisation threshold
  public static final String yamlForHPAWithCustomMetric = "apiVersion: autoscaling/v2beta1\n"
      + "kind: HorizontalPodAutoscaler\n"
      + "metadata:\n"
      + "  name: none\n"
      + "  namespace: none\n"
      + "spec:\n"
      + "  scaleTargetRef:\n"
      + "    kind: none\n"
      + "    name: none\n"
      + "  minReplicas: ${MIN_REPLICAS}\n"
      + "  maxReplicas: ${MAX_REPLICAS}\n"
      + "  metrics:\n"
      + "  - type: Resource\n"
      + "    resource:\n"
      + "      name: cpu\n"
      + "      targetAverageUtilization: ${UTILIZATION}\n";

  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    try {
      HorizontalPodAutoscaler horizontalPodAutoscaler = KubernetesHelper.loadYaml(
          yamlForHPAWithCustomMetric.replaceAll(MIN_REPLICAS, String.valueOf(minAutoscaleInstances.intValue()))
              .replaceAll(MAX_REPLICAS, String.valueOf(maxAutoscaleInstances.intValue()))
              .replaceAll(UTILIZATION, String.valueOf(targetCpuUtilizationPercentage.intValue())));

      return KubernetesHelper.toYaml(horizontalPodAutoscaler);
    } catch (IOException e) {
      throw new WingsException("Unable to generate Yaml String for Horizontal pod autoscalar");
    }
  }
}
