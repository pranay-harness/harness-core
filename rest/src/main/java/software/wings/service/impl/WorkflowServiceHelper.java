package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
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
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    try {
      String hpaYaml =
          yamlForHPAWithCustomMetric.replaceAll(MIN_REPLICAS, String.valueOf(minAutoscaleInstances.intValue()))
              .replaceAll(MAX_REPLICAS, String.valueOf(maxAutoscaleInstances.intValue()))
              .replaceAll(UTILIZATION, String.valueOf(targetCpuUtilizationPercentage.intValue()));
      HorizontalPodAutoscaler horizontalPodAutoscaler = KubernetesHelper.loadYaml(hpaYaml);
      if (horizontalPodAutoscaler == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse Horizontal Pod Autoscaler YAML: " + hpaYaml);
      }
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
      if (isNotEmpty(userVariables)) {
        serviceNames = getEntityNames(userVariables, SERVICE);
      }
      List<String> serviceIds = getTemplatizedIds(workflowVariables, serviceNames);
      List<String> templatizedServiceIds = orchestrationWorkflow.getTemplatizedServiceIds();
      List<String> workflowServiceIds = orchestrationWorkflow.getServiceIds();
      if (workflowServiceIds != null) {
        workflowServiceIds.stream()
            .filter(serviceId -> !templatizedServiceIds.contains(serviceId))
            .forEach(serviceIds::add);
      }
      return serviceResourceService.fetchServicesByUuids(workflow.getAppId(), serviceIds);
    } else {
      return workflow.getServices();
    }
  }

  public String resolveEnvironmentId(Workflow workflow, Map<String, String> workflowVariables) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (!workflow.checkEnvironmentTemplatized()) {
      return workflow.getEnvId();
    } else {
      if (isNotEmpty(workflowVariables)) {
        String envName = getTemplatizedEnvVariableName(orchestrationWorkflow);
        if (envName != null) {
          if (workflowVariables.get(envName) != null) {
            return workflowVariables.get(envName);
          }
        }
      }
    }
    throw new WingsException(
        "Workflow [" + workflow.getName() + "] environment parameterized. However, the value not supplied");
  }

  @SuppressFBWarnings("WMI_WRONG_MAP_ITERATOR")
  private List<String> getTemplatizedIds(Map<String, String> workflowVariables, List<String> entityNames) {
    List<String> entityIds = new ArrayList<>();
    if (workflowVariables != null) {
      Set<String> workflowVariableNames = workflowVariables.keySet();
      for (String variableName : workflowVariableNames) {
        if (entityNames.contains(variableName)) {
          entityIds.add(workflowVariables.get(variableName));
        }
      }
    }
    return entityIds;
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
      infraMappingNames = getEntityNames(userVariables, INFRASTRUCTURE_MAPPING);
    }
    List<String> infraMappingIds = getTemplatizedIds(workflowVariables, infraMappingNames);
    List<String> templatizedInfraMappingIds = orchestrationWorkflow.getTemplatizedInfraMappingIds();
    List<String> workflowInframappingIds = orchestrationWorkflow.getInfraMappingIds();
    if (workflowInframappingIds != null) {
      workflowInframappingIds.stream()
          .filter(infraMappingId -> !templatizedInfraMappingIds.contains(infraMappingId))
          .forEach(infraMappingIds::add);
    }
    return infrastructureMappingService.getInfraStructureMappingsByUuids(workflow.getAppId(), infraMappingIds);
  }

  private List<String> getEntityNames(List<Variable> userVariables, EntityType entityType) {
    return userVariables.stream()
        .filter(variable -> entityType.equals(variable.getEntityType()))
        .map(Variable::getName)
        .collect(toList());
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

  public static String getTemplatizedEnvVariableName(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow == null) {
      return null;
    }
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    if (isNotEmpty(userVariables)) {
      return userVariables.stream()
          .filter((Variable variable) -> ENVIRONMENT.equals(variable.getEntityType()))
          .map(Variable::getName)
          .findFirst()
          .orElse(null);
    }
    return null;
  }
}
