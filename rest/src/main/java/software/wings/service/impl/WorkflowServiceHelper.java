package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import software.wings.beans.Environment;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EnvironmentService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

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

  @Inject private EnvironmentService environmentService;
  @Inject private WingsPersistence wingsPersistence;

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

  public List<String> getKeywords(Workflow workflow) {
    List<Object> keywords = workflow.generateKeywords();
    if (workflow.getServices() != null) {
      workflow.getServices().forEach(service -> keywords.add(service.getName()));
    }
    if (workflow.getEnvId() != null) {
      Environment environment = environmentService.get(workflow.getAppId(), workflow.getEnvId(), false);
      if (environment != null) {
        keywords.add(environment.getName());
      }
    }
    if (workflow.getOrchestrationWorkflow() != null) {
      keywords.add(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    }

    if (workflow.isTemplatized()) {
      keywords.add("template");
    }
    return trimList(keywords);
  }

  /***
   *
   * @param templateExpressions
   * @return
   */
  public boolean isEnvironmentTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals("envId"));
  }

  /***
   *
   * @param templateExpressions
   * @return
   */
  public boolean isInfraTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals("infraMappingId"));
  }

  public void transformEnvTemplateExpressions(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow) {
    if (isNotEmpty(workflow.getTemplateExpressions())) {
      Optional<TemplateExpression> envExpression =
          workflow.getTemplateExpressions()
              .stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
              .findAny();
      if (envExpression.isPresent()) {
        orchestrationWorkflow.addToUserVariables(asList(envExpression.get()));
      }
    }
  }
}
