package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.KUBE_WORKFLOW_NAME;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.states.KubernetesSetup;
import software.wings.utils.Validator;

import java.util.Map;

@Singleton
public class WorkflowSampleDataProvider {
  @Inject private WorkflowService workflowService;

  public Workflow createKubeWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .withName(KUBE_WORKFLOW_NAME)
                            .withAppId(appId)
                            .withEnvId(envId)
                            .withServiceId(serviceId)
                            .withInfraMappingId(infraMappingId)
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);
    Validator.notNullCheck("Orchestration workflow not saved", savedWorkflow.getOrchestrationWorkflow());

    return savedWorkflow;
  }

  public Workflow templatizeEnvAndServiceInfra(Workflow workflow) {
    // Get Service Setup and update with loabalancer id
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhases().get(0);

    GraphNode kubernetesSetupNode =
        workflowPhase.getPhaseSteps()
            .get(0)
            .getSteps()
            .stream()
            .filter(graphNode -> StateType.KUBERNETES_SETUP.name().equals(graphNode.getType()))
            .findFirst()
            .orElse(null);

    // Add properties
    Map<String, Object> properties = kubernetesSetupNode.getProperties();
    properties.put(KubernetesSetup.PORT_KEY, 80);
    properties.put(KubernetesSetup.TARGET_PORT_KEY, 8080);
    properties.put(KubernetesSetup.SERVICE_TYPE_KEY, "LoadBalancer");
    properties.put(KubernetesSetup.PROTOCOL_KEY, "TCP");
    kubernetesSetupNode.setProperties(properties);

    TemplateExpression envTemplateExpression = TemplateExpression.builder()
                                                   .fieldName("envId")
                                                   .expression("${Environment}")
                                                   .metadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                                   .build();

    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .fieldName("infraMappingId")
                                                     .expression("${ServiceInfra_Kubernetes}")
                                                     .metadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                                     .build();

    workflow.setTemplateExpressions(asList(envTemplateExpression, infraTemplateExpression));

    return workflowService.updateWorkflow(workflow);
  }
}
