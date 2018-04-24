package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

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

  public List<Service> getResolvedServices(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isServiceTemplatized()) {
      List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
      List<String> serviceNames = new ArrayList<>();
      if (userVariables != null) {
        serviceNames = userVariables.stream()
                           .filter((Variable variable) -> SERVICE.equals(variable.getEntityType()))
                           .map(Variable::getName)
                           .collect(toList());
      }
      List<String> serviceIds = new ArrayList<>();
      if (workflowVariables != null) {
        Set<String> workflowVariableNames = workflowVariables.keySet();
        for (String variableName : workflowVariableNames) {
          if (serviceNames.contains(variableName)) {
            serviceIds.add(workflowVariables.get(variableName));
          }
        }
      }
      List<String> templatizedServiceIds = orchestrationWorkflow.getTemplatizedServiceIds();
      List<String> workflowServiceIds = workflow.getOrchestrationWorkflow().getServiceIds();
      if (workflowServiceIds != null) {
        workflowServiceIds.stream()
            .filter(serviceId -> !templatizedServiceIds.contains(serviceId))
            .forEach(serviceIds::add);
      }
      return serviceResourceService.getServicesByUuids(workflow.getAppId(), serviceIds);
    } else {
      return workflow.getServices();
    }
  }

  public List<InfrastructureMapping> getResolvedInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.isInfraMappingTemplatized()) {
      return resolvedTemplateInfraMappings(workflow, workflowVariables);
    } else {
      return infrastructureMappingService.getInfraStructureMappingsByUuids(
          workflow.getAppId(), orchestrationWorkflow.getInfraMappingIds());
    }
  }
  private List<InfrastructureMapping> resolvedTemplateInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    List<String> infraMappingNames = new ArrayList<>();
    if (userVariables != null) {
      infraMappingNames = userVariables.stream()
                              .filter(variable -> INFRASTRUCTURE_MAPPING.equals(variable.getEntityType()))
                              .map(Variable::getName)
                              .collect(toList());
    }
    List<String> infraMappingIds = new ArrayList<>();
    if (workflowVariables != null) {
      Set<String> workflowVariableNames = workflowVariables.keySet();
      for (String variableName : workflowVariableNames) {
        if (infraMappingNames.contains(variableName)) {
          infraMappingIds.add(workflowVariables.get(variableName));
        }
      }
    }
    List<String> templatizedInfraMappingIds = orchestrationWorkflow.getTemplatizedInfraMappingIds();
    List<String> workflowInframappingIds = orchestrationWorkflow.getInfraMappingIds();
    if (workflowInframappingIds != null) {
      workflowInframappingIds.stream()
          .filter(infraMappingId -> !templatizedInfraMappingIds.contains(infraMappingId))
          .forEach(infraMappingIds::add);
    }
    return infrastructureMappingService.getInfraStructureMappingsByUuids(workflow.getAppId(), infraMappingIds);
  }

  public boolean workflowHasSshInfraMapping(String appId, CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
      List<String> infraMappingIds = workflowPhases.stream()
                                         .filter(workflowPhase -> workflowPhase.getInfraMappingId() != null)
                                         .map(WorkflowPhase::getInfraMappingId)
                                         .collect(toList());
      return infrastructureMappingService.getInfraStructureMappingsByUuids(appId, infraMappingIds)
          .stream()
          .anyMatch((InfrastructureMapping infra)
                        -> AWS_SSH.name().equals(infra.getInfraMappingType())
                  || PHYSICAL_DATA_CENTER_SSH.name().equals(infra.getInfraMappingType()));
    }
    return false;
  }

  public List<String> getKeywords(Workflow workflow) {
    List<Object> keywords = workflow.generateKeywords();
    if (workflow.getEnvId() != null) {
      Environment environment = environmentService.get(workflow.getAppId(), workflow.getEnvId(), false);
      if (environment != null) {
        keywords.add(environment.getName());
      }
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
