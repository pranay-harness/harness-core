package software.wings.functional.vmss;

import static com.google.common.collect.Maps.newHashMap;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.ANIL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_DEPLOY;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_ROLLBACK;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_SWITCH_ROUTES;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.AZURE_VMSS_SWITCH_ROUTES_ROLLBACK;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.DEPLOY;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.DEPLOY_CONTAINERS;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.ROLLBACK_PREFIX;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.SETUP;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.VERIFY_SERVICE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.azure.model.AzureConstants;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.CDFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.InfrastructureDefinition;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureVMSSFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;

  private final Randomizer.Seed seed = new Randomizer.Seed(0);
  private Owners owners;
  private Service service;
  private Application application;
  private Environment environment;
  private final String WRAP_UP = "Wrap Up";

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
  }

  @Test
  @Owner(developers = ANIL, intermittent = true)
  @Category(CDFunctionalTests.class)
  @Ignore("still under progress. once complete will enable it")
  public void testVMSSBasicWorkflow() {
    Workflow workflow = getWorkflow(OrchestrationWorkflowType.BASIC);
    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, service.getAppId(), service.getArtifactStreamIds().get(0), 0);

    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), workflow.getUuid(), ImmutableList.of(artifact));
    verifyExecution(workflowExecution);
  }

  @Test
  @Owner(developers = ANIL, intermittent = true)
  @Category(CDFunctionalTests.class)
  @Ignore("still under progress. once complete will enable it")
  public void testVMSSCanaryWorkflow() {
    Workflow workflow = getWorkflow(OrchestrationWorkflowType.CANARY);
    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, service.getAppId(), service.getArtifactStreamIds().get(0), 0);

    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), workflow.getUuid(), ImmutableList.of(artifact));
    verifyExecution(workflowExecution);
  }

  @Test
  @Owner(developers = ANIL, intermittent = true)
  @Category(CDFunctionalTests.class)
  @Ignore("still under progress. once complete will enable it")
  public void testVMSSBlueGreenWorkflow() {
    Workflow workflow = getWorkflow(OrchestrationWorkflowType.BLUE_GREEN);
    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, service.getAppId(), service.getArtifactStreamIds().get(0), 0);

    WorkflowExecution workflowExecution = runWorkflow(
        bearerToken, application.getUuid(), environment.getUuid(), workflow.getUuid(), ImmutableList.of(artifact));
    verifyExecution(workflowExecution);
  }

  private void verifyExecution(WorkflowExecution workflowExecution) {
    ExecutionStatus status = workflowExecution.getStatus();
    String appId = workflowExecution.getAppId();
    String serviceId = workflowExecution.getServiceIds().get(0);
    String infraMappingId = workflowExecution.getInfraMappingIds().get(0);
    assertInstanceCount(status, appId, infraMappingId, workflowExecution.getInfraDefinitionIds().get(0));
  }

  @NotNull
  private Workflow getWorkflow(OrchestrationWorkflowType workflowType) {
    String serviceName = "Functional_Test_Azure_VMSS_Service";
    service = serviceGenerator.ensureAzureVMSSService(seed, owners, serviceName);
    assertThat(service).isNotNull();
    resetCache(service.getAccountId());

    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();

    InfrastructureDefinition infrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureType.AZURE_VMSS, bearerToken);
    assertThat(infrastructureDefinition).isNotNull();
    resetCache(service.getAccountId());

    Workflow workflow = generateWorkflow(workflowType, infrastructureDefinition);
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    resetCache(service.getAccountId());
    return savedWorkflow;
  }

  private Workflow generateWorkflow(
      OrchestrationWorkflowType workflowType, InfrastructureDefinition infrastructureDefinition) {
    WorkflowPhase workflowPhase = createWorkflowPhase(infrastructureDefinition, workflowType);
    WorkflowPhase rollbackPhase = createRollbackPhase(workflowPhase, workflowType);
    String workflowName = "vmss-" + workflowType.name() + "-" + System.currentTimeMillis();
    return getWorkflow(workflowName, infrastructureDefinition, workflowPhase, rollbackPhase);
  }

  private WorkflowPhase createWorkflowPhase(
      InfrastructureDefinition infrastructureDefinition, OrchestrationWorkflowType workflowType) {
    List<PhaseStep> phaseSteps = new ArrayList<>();

    Map<String, Object> defaultData = new HashMap<>();
    defaultData.put(AzureConstants.MIN_INSTANCES, AzureConstants.DEFAULT_AZURE_VMSS_MIN_INSTANCES);
    defaultData.put(AzureConstants.MAX_INSTANCES, AzureConstants.DEFAULT_AZURE_VMSS_MAX_INSTANCES);
    defaultData.put(AzureConstants.DESIRED_INSTANCES, AzureConstants.DEFAULT_AZURE_VMSS_DESIRED_INSTANCES);
    defaultData.put(AzureConstants.AUTO_SCALING_VMSS_TIMEOUT, AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN);
    defaultData.put(AzureConstants.BLUE_GREEN, Boolean.FALSE);

    phaseSteps.add(aPhaseStep(PhaseStepType.AZURE_VMSS_SETUP, SETUP)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.AZURE_VMSS_SETUP.name())
                                    .name(AZURE_VMSS_SETUP)
                                    .properties(defaultData)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(PhaseStepType.AZURE_VMSS_DEPLOY, DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(StateType.AZURE_VMSS_DEPLOY.name())
                                    .name(AZURE_VMSS_DEPLOY)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceCount", 100)
                                                    .put("instanceUnitType", InstanceUnitType.PERCENTAGE)
                                                    .build())
                                    .build())
                       .build());

    if (BLUE_GREEN == workflowType) {
      Map<String, Object> defaultDataSwitchRoutes = newHashMap();
      defaultDataSwitchRoutes.put("downsizeOldVMSS", true);
      phaseSteps.add(aPhaseStep(PhaseStepType.AZURE_VMSS_SWITCH_ROUTES, AZURE_VMSS_SWITCH_ROUTES)
                         .addStep(GraphNode.builder()
                                      .id(generateUuid())
                                      .type(PhaseStepType.AZURE_VMSS_SWITCH_ROUTES.name())
                                      .name(AZURE_VMSS_SWITCH_ROUTES)
                                      .properties(defaultDataSwitchRoutes)
                                      .build())
                         .build());
    }
    phaseSteps.add(aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE).build());
    phaseSteps.add(aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).build());

    return aWorkflowPhase()
        .uuid(generateUuid())
        .serviceId(service.getUuid())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .phaseSteps(phaseSteps)
        .build();
  }

  private WorkflowPhase createRollbackPhase(WorkflowPhase workflowPhase, OrchestrationWorkflowType workflowType) {
    PhaseStepType azureVMSSPhaseStepRollback =
        (BLUE_GREEN == workflowType) ? PhaseStepType.AZURE_VMSS_SWITCH_ROLLBACK : PhaseStepType.AZURE_VMSS_ROLLBACK;

    String azureVMSSStateType = (BLUE_GREEN == workflowType) ? StateType.AZURE_VMSS_SWITCH_ROUTES_ROLLBACK.name()
                                                             : StateType.AZURE_VMSS_ROLLBACK.name();

    String rollbackName = (BLUE_GREEN == workflowType) ? AZURE_VMSS_ROLLBACK : AZURE_VMSS_SWITCH_ROUTES_ROLLBACK;

    WorkflowPhaseBuilder rollbackPhaseBuilder = aWorkflowPhase()
                                                    .name(ROLLBACK_PREFIX + workflowPhase.getName())
                                                    .deploymentType(workflowPhase.getDeploymentType())
                                                    .rollback(true)
                                                    .phaseNameForRollback(workflowPhase.getName())
                                                    .serviceId(workflowPhase.getServiceId())
                                                    .computeProviderId(workflowPhase.getComputeProviderId())
                                                    .infraDefinitionId(workflowPhase.getInfraDefinitionId());

    return rollbackPhaseBuilder
        .phaseSteps(asList(aPhaseStep(azureVMSSPhaseStepRollback, rollbackName)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(azureVMSSStateType)
                                            .name(rollbackName)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(DEPLOY)
                               .withStatusForRollback(SUCCESS)
                               .withRollback(true)
                               .build(),
            aPhaseStep(PhaseStepType.VERIFY_SERVICE, VERIFY_SERVICE)
                .withPhaseStepNameForRollback(DEPLOY_CONTAINERS)
                .withStatusForRollback(SUCCESS)
                .withRollback(true)
                .build(),
            aPhaseStep(PhaseStepType.WRAP_UP, WRAP_UP).withRollback(true).build()))
        .build();
  }

  @NotNull
  private Workflow getWorkflow(String workflowName, InfrastructureDefinition infrastructureDefinition,
      WorkflowPhase workflowPhase, WorkflowPhase rollbackPhase) {
    Map<String, WorkflowPhase> rollbackMap = new HashMap<>();
    rollbackMap.put(workflowPhase.getUuid(), rollbackPhase);
    return aWorkflow()
        .name(workflowName)
        .appId(service.getAppId())
        .serviceId(service.getUuid())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(aCanaryOrchestrationWorkflow()
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                                   .withWorkflowPhases(Collections.singletonList(workflowPhase))
                                   .withRollbackWorkflowPhaseIdMap(rollbackMap)
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                                   .build())
        .build();
  }
}
