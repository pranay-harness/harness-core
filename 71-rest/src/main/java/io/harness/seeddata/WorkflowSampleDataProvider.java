package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.K8S_BASIC_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CANARY_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_ROLLING_WORKFLOW_NAME;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.sm.states.ContainerServiceSetup.DESIRED_INSTANCE_COUNT_KEY;
import static software.wings.sm.states.ContainerServiceSetup.FIXED_INSTANCES;
import static software.wings.sm.states.KubernetesDeploy.INSTANCE_COUNT_KEY;
import static software.wings.sm.states.KubernetesDeploy.INSTANCE_UNIT_TYPE_KEY;
import static software.wings.sm.states.KubernetesSetup.PORT_KEY;
import static software.wings.sm.states.KubernetesSetup.PROTOCOL_KEY;
import static software.wings.sm.states.KubernetesSetup.SERVICE_TYPE_KEY;
import static software.wings.sm.states.KubernetesSetup.TARGET_PORT_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.utils.Validator;

import java.util.Map;

@Singleton
public class WorkflowSampleDataProvider {
  @Inject private WorkflowService workflowService;

  public String createK8sBasicWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .name(K8S_BASIC_WORKFLOW_NAME)
                            .appId(appId)
                            .envId(envId)
                            .serviceId(serviceId)
                            .infraMappingId(infraMappingId)
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);
    Validator.notNullCheck("Orchestration workflow not saved", savedWorkflow.getOrchestrationWorkflow());

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    WorkflowPhase workflowPhase = canaryOrchestrationWorkflow.getWorkflowPhases().get(0);

    setK8sSetupContainer(workflowPhase, "1");

    workflowService.updateWorkflow(workflow);

    return savedWorkflow.getUuid();
  }

  public String createK8sCanaryWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .name(K8S_CANARY_WORKFLOW_NAME)
                            .appId(appId)
                            .envId(envId)
                            .serviceId(serviceId)
                            .infraMappingId(infraMappingId)
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);
    Validator.notNullCheck("Orchestration workflow not saved", savedWorkflow.getOrchestrationWorkflow());

    // Attach workflow first Workflow Phase
    WorkflowPhase workflowPhase = workflowService.createWorkflowPhase(
        appId, savedWorkflow.getUuid(), aWorkflowPhase().serviceId(serviceId).infraMappingId(infraMappingId).build());

    setK8sSetupContainer(workflowPhase, "2");
    setK8sUpgradeContainer(workflowPhase, "50");

    workflowService.updateWorkflowPhase(appId, savedWorkflow.getUuid(), workflowPhase);

    // Attach Second Workflow Phase
    WorkflowPhase secondPhase = workflowService.createWorkflowPhase(
        appId, savedWorkflow.getUuid(), aWorkflowPhase().serviceId(serviceId).infraMappingId(infraMappingId).build());

    setK8sUpgradeContainer(secondPhase, "100");

    workflowService.updateWorkflowPhase(appId, savedWorkflow.getUuid(), secondPhase);

    return savedWorkflow.getUuid();
  }

  public String createK8sV2RollingWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .name(K8S_ROLLING_WORKFLOW_NAME)
                            .appId(appId)
                            .envId(envId)
                            .serviceId(serviceId)
                            .infraMappingId(infraMappingId)
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();

    workflow.getOrchestrationWorkflow().setOrchestrationWorkflowType(OrchestrationWorkflowType.ROLLING);

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);
    Validator.notNullCheck("Orchestration workflow not saved", savedWorkflow.getOrchestrationWorkflow());

    return savedWorkflow.getUuid();
  }

  public String createK8sV2CanaryWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .name(K8S_CANARY_WORKFLOW_NAME)
                            .appId(appId)
                            .envId(envId)
                            .serviceId(serviceId)
                            .infraMappingId(infraMappingId)
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();

    workflow.getOrchestrationWorkflow().setOrchestrationWorkflowType(OrchestrationWorkflowType.CANARY);

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);
    Validator.notNullCheck("Orchestration workflow not saved", savedWorkflow.getOrchestrationWorkflow());

    return savedWorkflow.getUuid();
  }

  private void setK8sSetupContainer(WorkflowPhase workflowPhase, String instanceCount) {
    // Get Service Setup and update with loabalancer id
    final PhaseStep containerSetup =
        workflowPhase.getPhaseSteps()
            .stream()
            .filter(phaseStep -> phaseStep.getPhaseStepType().equals(PhaseStepType.CONTAINER_SETUP))
            .findFirst()
            .orElse(null);
    Validator.notNullCheck("Container Setup Phase Step required", containerSetup);

    GraphNode kubernetesSetupNode =
        containerSetup.getSteps()
            .stream()
            .filter(graphNode -> StateType.KUBERNETES_SETUP.name().equals(graphNode.getType()))
            .findFirst()
            .orElse(null);

    Validator.notNullCheck("Kebernetes Setup Step required", containerSetup);

    // Add properties
    Map<String, Object> properties = kubernetesSetupNode.getProperties();
    properties.put(FIXED_INSTANCES, instanceCount);
    properties.put(DESIRED_INSTANCE_COUNT_KEY, FIXED_INSTANCES);
    properties.put(PORT_KEY, 80);
    properties.put(TARGET_PORT_KEY, 8080);
    properties.put(SERVICE_TYPE_KEY, "LoadBalancer");
    properties.put(PROTOCOL_KEY, "TCP");

    kubernetesSetupNode.setProperties(properties);
  }

  private void setK8sUpgradeContainer(WorkflowPhase workflowPhase, String percentage) {
    final PhaseStep containerDeploy =
        workflowPhase.getPhaseSteps()
            .stream()
            .filter(phaseStep -> phaseStep.getPhaseStepType().equals(PhaseStepType.CONTAINER_DEPLOY))
            .findFirst()
            .orElse(null);
    Validator.notNullCheck("Container Deploy Phase Step required", containerDeploy);

    GraphNode upgradeContainerNode =
        containerDeploy.getSteps()
            .stream()
            .filter(graphNode -> StateType.KUBERNETES_DEPLOY.name().equals(graphNode.getType()))
            .findFirst()
            .orElse(null);

    Validator.notNullCheck("Kubernetes Deploy Step required", upgradeContainerNode);

    Map<String, Object> properties = upgradeContainerNode.getProperties();
    properties.put(INSTANCE_UNIT_TYPE_KEY, InstanceUnitType.PERCENTAGE);
    properties.put(INSTANCE_COUNT_KEY, percentage);
    upgradeContainerNode.setProperties(properties);
  }
}
