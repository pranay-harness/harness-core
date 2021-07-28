package io.harness.functional.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.SettingGenerator.Settings.AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.PRAKHAR;

import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.CDFunctionalTests;
import io.harness.category.element.FunctionalTests;
import io.harness.delegate.beans.pcf.ResizeStrategy;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.InfrastructureDefinitionGenerator.InfrastructureDefinitions;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
@Slf4j
public class EcsWorkflowFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private WorkflowUtils workflowUtils;

  final Seed seed = new Seed(0);
  Owners owners;

  // @TODO please remove this and use actual constants when constants class is refactored
  final String SETUP_CONTAINER_CONSTANT = "Setup Container";
  final String PRE_DEPLOYMENT_CONSTANT = "Pre-Deployment";
  final String ECS_DAEMON_SERVICE_SETUP_NAME = "ECS Daemon Service Setup";
  final String POST_DEPLOYMENT_CONSTANT = "Post-Deployment";
  final String WRAP_UP_CONSTANT = "Wrap Up";
  final String ECS_SERVICE_SETUP_CONSTANT = "ECS Service Setup";
  final String UPGRADE_CONTAINERS_CONSTANT = "Upgrade Containers";
  final String DEPLOY_CONTAINERS_CONSTANT = "Deploy Containers";
  final String APPROVAL_CONSTANT = "Approval";
  final String SWAP_TARGET_GROUPS = "Swap Target Groups";
  final String CHANGE_ROUTE_53_WEIGHTS = "Change Route 53 Weights";
  final String SWAP_ROUTE53_DNS = "Swap Route 53 DNS";

  private Application application;
  private Service service;
  private Environment environment;
  private InfrastructureDefinition infrastructureDefinition;
  private SettingAttribute awsSettingAttribute;
  private Artifact artifact;
  private ArtifactStream artifactStream;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    service = serviceGenerator.ensureEcsTest(seed, owners, "Func_Test_Ecs_Service");
    assertThat(service).isNotNull();

    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    assertThat(environment).isNotNull();

    infrastructureDefinition = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitions.ECS_DEPLOYMENT_FUNCTIONAL_TEST);
    assertThat(infrastructureDefinition).isNotNull();

    awsSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    artifactStream = artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_DOCKER);
    assertThat(artifactStream).isNotNull();

    artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, application.getUuid(), artifactStream.getUuid(), 0);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateBasicEcsWorkflow() throws Exception {
    Workflow workflow = getBasicEcsEc2TypeWorkflow();
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    // Test running the workflow
    assertExecution(savedWorkflow);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(CDFunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateDaemonEcsWorkflow() throws Exception {
    Workflow workflow = getEcsEc2TypeDaemonWorkflow();
    Workflow savedWorkflow = createAndAssertWorkflow(workflow);
    // Test running the workflow
    assertExecution(savedWorkflow);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(CDFunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateCanaryEcsWorkflow() throws Exception {
    Workflow workflow = getEcsEc2TypeCanaryWorkflow();
    Workflow savedWorkflow = createAndAssertWorkflow(workflow);

    // Test running the workflow
    assertExecution(savedWorkflow);
  }

  @Test
  @Owner(developers = PRAKHAR)
  @Category(CDFunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateCanaryEcsRollbackWorkflow() throws Exception {
    Workflow workflow = getEcsEc2TypeCanaryRollbackWorkflow();
    Workflow savedWorkflow = createAndAssertWorkflow(workflow);

    // Test running the workflow
    WorkflowExecution workflowExecution = assertExecutionWithStatus(savedWorkflow, ExecutionStatus.FAILED);
    workflowUtils.assertRollbackInWorkflowExecution(workflowExecution);
  }

  private Workflow createAndAssertWorkflow(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);
    return savedWorkflow;
  }

  private Workflow getEcsEc2TypeDaemonWorkflow() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_DAEMON_SERVICE_SETUP.name())
                                    .name(ECS_DAEMON_SERVICE_SETUP_NAME)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("useLoadBalancer", false)
                                                    .put("ecsServiceName", "${app.name}__${service.name}__DAEMON")
                                                    .put("serviceSteadyStateTimeout", 10)
                                                    .build())
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflow()
        .name("Daemon ECS" + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(
            aBuildOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.ECS)
                                      .daemonSet(true)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .computeProviderId(awsSettingAttribute.getUuid())
                                      .phaseSteps(phaseSteps)
                                      .build())
                .build())
        .build();
  }

  private Workflow getEcsEc2TypeCanaryWorkflow() {
    List<PhaseStep> phaseSteps1 = new ArrayList<>();

    phaseSteps1.add(workflowUtils.ecsContainerSetupPhaseStep());
    phaseSteps1.add(workflowUtils.ecsContainerDeployPhaseStep());

    phaseSteps1.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    List<PhaseStep> phaseSteps2 = new ArrayList<>();

    phaseSteps2.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
                        .addStep(GraphNode.builder()
                                     .id(generateUuid())
                                     .type(ECS_SERVICE_DEPLOY.name())
                                     .name(UPGRADE_CONTAINERS_CONSTANT)
                                     .properties(ImmutableMap.<String, Object>builder()
                                                     .put("instanceUnitType", "PERCENTAGE")
                                                     .put("instanceCount", 100)
                                                     .put("downsizeInstanceUnitType", "PERCENTAGE")
                                                     .put("downsizeInstanceCount", 0)
                                                     .build())
                                     .build())
                        .build());

    phaseSteps2.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflow()
        .name("Canary ECS" + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(
            aBuildOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .name("0% - 50%")
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.ECS)
                                      .daemonSet(false)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .computeProviderId(awsSettingAttribute.getUuid())
                                      .phaseSteps(phaseSteps1)
                                      .build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .name("50% - 100%")
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.ECS)
                                      .daemonSet(false)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .computeProviderId(awsSettingAttribute.getUuid())
                                      .phaseSteps(phaseSteps2)
                                      .build())
                .build())
        .build();
  }

  private Workflow getEcsEc2TypeCanaryRollbackWorkflow() {
    List<PhaseStep> phaseSteps1 = new ArrayList<>();
    phaseSteps1.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
                        .addStep(GraphNode.builder()
                                     .id(generateUuid())
                                     .type(ECS_SERVICE_SETUP.name())
                                     .name(ECS_SERVICE_SETUP_CONSTANT)
                                     .properties(ImmutableMap.<String, Object>builder()
                                                     .put("fixedInstances", "2")
                                                     .put("useLoadBalancer", false)
                                                     .put("ecsServiceName", "${app.name}__${service.name}__BASIC")
                                                     .put("desiredInstanceCount", "fixedInstances")
                                                     .put("resizeStrategy", ResizeStrategy.DOWNSIZE_OLD_FIRST)
                                                     .put("serviceSteadyStateTimeout", 10)
                                                     .build())
                                     .build())
                        .build());
    phaseSteps1.add(workflowUtils.ecsContainerDeployPhaseStep());
    phaseSteps1.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    List<PhaseStep> phaseSteps2 = new ArrayList<>();
    List<String> userGroups = Collections.singletonList("uK63L5CVSAa1-BkC4rXoRg");
    phaseSteps2.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
                        .addStep(GraphNode.builder()
                                     .id(generateUuid())
                                     .type(ECS_SERVICE_DEPLOY.name())
                                     .name(UPGRADE_CONTAINERS_CONSTANT)
                                     .properties(ImmutableMap.<String, Object>builder()
                                                     .put("instanceUnitType", "PERCENTAGE")
                                                     .put("instanceCount", 100)
                                                     .put("downsizeInstanceUnitType", "PERCENTAGE")
                                                     .put("downsizeInstanceCount", 0)
                                                     .build())
                                     .build())
                        .addStep(GraphNode.builder()
                                     .id(generateUuid())
                                     .type(APPROVAL.name())
                                     .name(APPROVAL_CONSTANT)
                                     .properties(ImmutableMap.<String, Object>builder()
                                                     .put("timeoutMillis", 60000)
                                                     .put("approvalStateType", "USER_GROUP")
                                                     .put("userGroups", userGroups)
                                                     .build())
                                     .build())
                        .build());
    phaseSteps2.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    List<PhaseStep> rollbackPhaseStep1 = new ArrayList<>();
    rollbackPhaseStep1.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
                               .withStatusForRollback(ExecutionStatus.SUCCESS)
                               .withRollback(true)
                               .withPhaseStepNameForRollback(DEPLOY_CONTAINERS_CONSTANT)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .name("Rollback Containers")
                                            .type(ECS_SERVICE_ROLLBACK.name())
                                            .rollback(true)
                                            .origin(true)
                                            .properties(ImmutableMap.<String, Object>builder().build())
                                            .build())
                               .build());
    rollbackPhaseStep1.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).withRollback(true).build());

    List<PhaseStep> rollbackPhaseStep2 = new ArrayList<>();
    rollbackPhaseStep2.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
                               .withStatusForRollback(ExecutionStatus.SUCCESS)
                               .withRollback(true)
                               .withPhaseStepNameForRollback(DEPLOY_CONTAINERS_CONSTANT)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .name("Rollback Containers")
                                            .type(ECS_SERVICE_ROLLBACK.name())
                                            .rollback(true)
                                            .origin(true)
                                            .properties(ImmutableMap.<String, Object>builder().build())
                                            .build())
                               .build());
    rollbackPhaseStep2.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).withRollback(true).build());

    WorkflowPhase workflowPhase1 = aWorkflowPhase()
                                       .name("0% - 50%")
                                       .serviceId(service.getUuid())
                                       .deploymentType(DeploymentType.ECS)
                                       .daemonSet(false)
                                       .infraDefinitionId(infrastructureDefinition.getUuid())
                                       .infraDefinitionName(infrastructureDefinition.getName())
                                       .computeProviderId(awsSettingAttribute.getUuid())
                                       .phaseSteps(phaseSteps1)
                                       .build();

    WorkflowPhase workflowPhase2 = aWorkflowPhase()
                                       .name("50% - 100%")
                                       .serviceId(service.getUuid())
                                       .deploymentType(DeploymentType.ECS)
                                       .daemonSet(false)
                                       .infraDefinitionId(infrastructureDefinition.getUuid())
                                       .infraDefinitionName(infrastructureDefinition.getName())
                                       .computeProviderId(awsSettingAttribute.getUuid())
                                       .phaseSteps(phaseSteps2)
                                       .build();

    Map<String, WorkflowPhase> workflowPhaseIdMap = new HashMap<>();
    workflowPhaseIdMap.put(workflowPhase1.getUuid(),
        aWorkflowPhase()
            .rollback(true)
            .phaseSteps(rollbackPhaseStep1)
            .phaseNameForRollback("0% - 50%")
            .name("Rollback 0% - 50%")
            .serviceId(service.getUuid())
            .deploymentType(DeploymentType.ECS)
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .computeProviderId(awsSettingAttribute.getUuid())
            .build());
    workflowPhaseIdMap.put(workflowPhase2.getUuid(),
        aWorkflowPhase()
            .rollback(true)
            .phaseSteps(rollbackPhaseStep2)
            .phaseNameForRollback("50% - 100%")
            .name("Rollback 50% - 100%")
            .serviceId(service.getUuid())
            .deploymentType(DeploymentType.ECS)
            .infraDefinitionId(infrastructureDefinition.getUuid())
            .computeProviderId(awsSettingAttribute.getUuid())
            .build());

    return aWorkflow()
        .name("Canary ECS Rollback" + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                .addWorkflowPhase(workflowPhase1)
                .addWorkflowPhase(workflowPhase2)
                .withRollbackWorkflowPhaseIdMap(workflowPhaseIdMap)
                .build())
        .build();
  }

  private Workflow getBasicEcsEc2TypeWorkflow() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(aPhaseStep(CONTAINER_SETUP, SETUP_CONTAINER_CONSTANT)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_SERVICE_SETUP.name())
                                    .name(ECS_SERVICE_SETUP_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("fixedInstances", "1")
                                                    .put("useLoadBalancer", false)
                                                    .put("ecsServiceName", "${app.name}__${service.name}__BASIC")
                                                    .put("desiredInstanceCount", "fixedInstances")
                                                    .put("resizeStrategy", ResizeStrategy.DOWNSIZE_OLD_FIRST)
                                                    .put("serviceSteadyStateTimeout", 10)
                                                    .build())
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(CONTAINER_DEPLOY, DEPLOY_CONTAINERS_CONSTANT)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ECS_SERVICE_DEPLOY.name())
                                    .name(UPGRADE_CONTAINERS_CONSTANT)
                                    .properties(ImmutableMap.<String, Object>builder()
                                                    .put("instanceUnitType", "PERCENTAGE")
                                                    .put("instanceCount", 100)
                                                    .put("downsizeInstanceUnitType", "PERCENTAGE")
                                                    .put("downsizeInstanceCount", 0)
                                                    .build())
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(WRAP_UP, WRAP_UP_CONSTANT).build());

    return aWorkflow()
        .name("Basic ECS" + System.currentTimeMillis())
        .workflowType(WorkflowType.ORCHESTRATION)
        .appId(service.getAppId())
        .envId(infrastructureDefinition.getEnvId())
        .infraDefinitionId(infrastructureDefinition.getUuid())
        .serviceId(service.getUuid())
        .orchestrationWorkflow(
            aBuildOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, PRE_DEPLOYMENT_CONSTANT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, POST_DEPLOYMENT_CONSTANT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .serviceId(service.getUuid())
                                      .deploymentType(DeploymentType.ECS)
                                      .daemonSet(false)
                                      .infraDefinitionId(infrastructureDefinition.getUuid())
                                      .infraDefinitionName(infrastructureDefinition.getName())
                                      .computeProviderId(awsSettingAttribute.getUuid())
                                      .phaseSteps(phaseSteps)
                                      .build())
                .build())
        .build();
  }

  private void assertExecution(Workflow savedWorkflow) {
    assertExecutionWithStatus(savedWorkflow, ExecutionStatus.SUCCESS);
  }

  private WorkflowExecution assertExecutionWithStatus(Workflow savedWorkflow, ExecutionStatus executionStatus) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setArtifacts(Arrays.asList(artifact));
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());

    log.info("Invoking workflow execution");
    //    WorkflowExecution workflowExecution = workflowExecutionService.triggerEnvExecution(service.getApplicationId(),
    //        infrastructureMapping.getEnvId(), executionArgs, Trigger.builder().name("adwait").uuid("uuId").build());

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();
    log.info("Waiting for execution to finish");

    Awaitility.await()
        .atMost(600, TimeUnit.SECONDS)
        .pollInterval(15, TimeUnit.SECONDS)
        .until(()
                   -> Setup.portal()
                          .auth()
                          .oauth2(bearerToken)
                          .queryParam("appId", application.getUuid())
                          .get("/executions/" + workflowExecution.getUuid())
                          .jsonPath()
                          .<String>getJsonObject("resource.status")
                          .equals(executionStatus.name()));

    WorkflowExecution completedWorkflowExecution =
        workflowExecutionService.getExecutionDetails(application.getUuid(), workflowExecution.getUuid(), true, false);
    log.info("ECs Execution status: " + completedWorkflowExecution.getStatus());
    assertThat(executionStatus == completedWorkflowExecution.getStatus());

    return completedWorkflowExecution;
  }
}
