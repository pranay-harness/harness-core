package software.wings.service.impl.workflow;

import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.api.CloudProviderType.AWS;
import static software.wings.api.CloudProviderType.GCP;
import static software.wings.api.CloudProviderType.PHYSICAL_DATA_CENTER;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.container.EcsServiceSpecification.ECS_REPLICA_SCHEDULING_STRATEGY;
import static software.wings.beans.workflow.StepSkipStrategy.Scope.ALL_STEPS;
import static software.wings.beans.workflow.StepSkipStrategy.Scope.SPECIFIC_STEPS;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.ECS_DAEMON_SCHEDULING_STRATEGY;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_LISTENER_UPDATE;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP_ROLLBACK;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkflowServiceHelperTest extends WingsBaseTest {
  private static String envId = generateUuid();

  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ArtifactStreamService artifactStreamService;
  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetHPAYamlStringWithCustomMetric() throws Exception {
    WorkflowServiceHelper workflowServiceHelper = new WorkflowServiceHelper();
    String hpaString = workflowServiceHelper.getHPAYamlStringWithCustomMetric(2, 4, 80);

    String yamlForHPAWithCustomMetric = "apiVersion: autoscaling/v2beta1\n"
        + "kind: HorizontalPodAutoscaler\n"
        + "metadata:\n"
        + "  name: hpa-name\n"
        + "spec:\n"
        + "  scaleTargetRef:\n"
        + "    apiVersion: extensions/v1beta1\n"
        + "    kind: Deployment\n"
        + "    name: target-name\n"
        + "  minReplicas: 2\n"
        + "  maxReplicas: 4\n"
        + "  metrics:\n"
        + "  - type: Resource\n"
        + "    resource:\n"
        + "      name: cpu\n"
        + "      targetAverageUtilization: 80\n";

    assertThat(hpaString).isEqualTo(yamlForHPAWithCustomMetric);
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldValidateK8ServiceType() throws Exception {
    String oldServiceId = SERVICE_ID + "_Old";

    Service service = Service.builder().name(SERVICE_NAME).isK8sV2(true).uuid(SERVICE_ID).build();
    Service oldService = Service.builder().name(SERVICE_NAME + "_Old").isK8sV2(false).uuid(oldServiceId).build();

    doReturn(service).when(serviceResourceService).get(APP_ID, SERVICE_ID, false);
    doReturn(oldService).when(serviceResourceService).get(APP_ID, oldServiceId, false);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> workflowServiceHelper.validateServiceCompatibility(APP_ID, SERVICE_ID, oldServiceId));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testIsDaemonSchedulingStrategy() throws Exception {
    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    boolean isDaemonSchedulingStrategy =
        workflowServiceHelper.isDaemonSchedulingStrategy(APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), BASIC);
    assertThat(isDaemonSchedulingStrategy).isTrue();

    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.CANARY);
    assertThat(isDaemonSchedulingStrategy).isFalse();
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.ROLLING);
    assertThat(isDaemonSchedulingStrategy).isFalse();
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.CUSTOM);
    assertThat(isDaemonSchedulingStrategy).isFalse();

    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    isDaemonSchedulingStrategy =
        workflowServiceHelper.isDaemonSchedulingStrategy(APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), BASIC);
    assertThat(isDaemonSchedulingStrategy).isFalse();

    serviceSpecification.resetToDefaultSpecification();
    isDaemonSchedulingStrategy =
        workflowServiceHelper.isDaemonSchedulingStrategy(APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), BASIC);
    assertThat(isDaemonSchedulingStrategy).isFalse();

    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    isDaemonSchedulingStrategy =
        workflowServiceHelper.isDaemonSchedulingStrategy(APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), BASIC);
    assertThat(isDaemonSchedulingStrategy).isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForSpotinstTrafficShiftAlb() {
    ServiceResourceService mockServiceResourceService = mock(ServiceResourceService.class);
    WorkflowServiceHelper helper = spy(WorkflowServiceHelper.class);
    on(helper).set("serviceResourceService", mockServiceResourceService);
    Service service = Service.builder().appId(APP_ID).build();
    doReturn(service).when(mockServiceResourceService).getWithDetails(anyString(), anyString());
    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    helper.generateNewWorkflowPhaseStepsForSpotinstAlbTrafficShift(APP_ID, workflowPhase);
    verifyPhase(workflowPhase,
        asList(StateType.SPOTINST_ALB_SHIFT_SETUP.name(), StateType.SPOTINST_ALB_SHIFT_DEPLOY.name()), 5);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForSpotinstAlbTrafficShift() {
    WorkflowServiceHelper helper = spy(WorkflowServiceHelper.class);
    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    WorkflowPhase rollbackPhase = helper.generateRollbackWorkflowPhaseForSpotinstAlbTrafficShift(workflowPhase);
    verifyPhase(rollbackPhase, singletonList(StateType.SPOTINST_LISTENER_ALB_SHIFT_ROLLBACK.name()), 3);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification specification = EcsServiceSpecification.builder().build();
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(specification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(APP_ID, workflowPhase, true, BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    specification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(APP_ID, workflowPhase, true, BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    // Non basic workflows should not respect DEAMON scheduling strategy
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CANARY);
    verifyPhase(workflowPhase, asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    specification.resetToDefaultSpecification();
    specification.setServiceSpecJson(specification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CUSTOM);
    verifyPhase(workflowPhase, asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_BG() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification specification = EcsServiceSpecification.builder().build();
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(specification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECSBlueGreen(APP_ID, workflowPhase, true);
    verifyPhase(workflowPhase,
        asList(new String[] {ECS_BG_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name(), null, ECS_LISTENER_UPDATE.name()}),
        5);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(APP_ID, workflowPhase, true, BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(APP_ID, workflowPhase, true, BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    // Non basic workflows should not respect DEAMON scheduling strategy
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.CANARY);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.CUSTOM);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_BG() {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcsBlueGreen(APP_ID, workflowPhase, BASIC);
    verifyPhase(rollbackPhase,
        asList(new String[] {StateType.ECS_LISTENER_UPDATE_ROLLBACK.name(), ECS_SERVICE_ROLLBACK.name()}), 4);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    EcsServiceSpecification ecsServiceSpecification = EcsServiceSpecification.builder().build();
    ecsServiceSpecification.setServiceSpecJson(null);
    ecsServiceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(ecsServiceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase phase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase = workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);

    ecsServiceSpecification.resetToDefaultSpecification();
    ecsServiceSpecification.setServiceSpecJson(
        ecsServiceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    phase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    rollbackPhase = workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTestWorkflowOverrideVariables() {
    List<Variable> variables =
        asList(aVariable().name("Environment").build(), aVariable().name("OverrideVariable").build(),
            aVariable().name("MyOwnVariable").build(), aVariable().name("NewlyCreatedVariable").build());

    Map<String, String> pipelineVariables = new HashMap<>();
    pipelineVariables.put("Env", "EnvironmentValue");
    pipelineVariables.put("Service", "ServiceValue");
    pipelineVariables.put("OverrideVariable", "${app.name}");
    pipelineVariables.put("NewlyCreatedVariable", "NewlyCreatedVariableValue");

    Map<String, String> pipelineStepVariables = new HashMap<>();
    pipelineStepVariables.put("Environment", "${Env}");
    pipelineStepVariables.put("OverrideVariable", "myValue");
    pipelineStepVariables.put("MyOwnVariable", "MyOwnVariableValue");

    Map<String, String> overrideWorkflowVariables =
        WorkflowServiceHelper.overrideWorkflowVariables(variables, pipelineStepVariables, pipelineVariables);

    // Case 1: Override the workflow variable name
    assertThat(overrideWorkflowVariables).containsKeys("Environment", "OverrideVariable", "MyOwnVariable");
    assertThat(overrideWorkflowVariables).containsValues("EnvironmentValue", "${app.name}", "MyOwnVariableValue");
    assertThat(overrideWorkflowVariables).doesNotContainKey("Service");
    assertThat(overrideWorkflowVariables).containsKey("NewlyCreatedVariable");
    assertThat(overrideWorkflowVariables.get("NewlyCreatedVariable")).isEqualTo("NewlyCreatedVariableValue");

    // Case 2: No pipeline variables .. should return all the workflow variables
    overrideWorkflowVariables =
        WorkflowServiceHelper.overrideWorkflowVariables(variables, pipelineStepVariables, new HashMap<>());
    assertThat(overrideWorkflowVariables).containsKeys("Environment", "OverrideVariable", "MyOwnVariable");
    assertThat(overrideWorkflowVariables).containsValues("${Env}", "myValue", "MyOwnVariableValue");

    // Case 3: Pipeline Step has variable, however Workflow does not have that variable anymore
    variables = asList(aVariable().name("Environment").build(), aVariable().name("OverrideVariable").build());
    overrideWorkflowVariables =
        WorkflowServiceHelper.overrideWorkflowVariables(variables, pipelineStepVariables, pipelineVariables);
    assertThat(overrideWorkflowVariables).doesNotContainKey("MyOwnVariable");

    // Case 4: Pipeline step does not have variable, however pipeline variable has the value
    variables = asList(aVariable().name("Environment").build(), aVariable().name("OverrideVariable").build(),
        aVariable().name("MyOwnVariable").build());
    overrideWorkflowVariables =
        WorkflowServiceHelper.overrideWorkflowVariables(variables, pipelineStepVariables, pipelineVariables);
    assertThat(overrideWorkflowVariables).doesNotContainKey("Service");
    assertThat(overrideWorkflowVariables.get("Service")).isNullOrEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testWorkflowHasSshDeploymentPhase() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow_ssh =
        aCanaryOrchestrationWorkflow()
            .withWorkflowPhases(Arrays.asList(aWorkflowPhase().deploymentType(SSH).build()))
            .build();
    assertThat(workflowServiceHelper.workflowHasSshDeploymentPhase(canaryOrchestrationWorkflow_ssh)).isTrue();

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow_withoutssh =
        aCanaryOrchestrationWorkflow()
            .withWorkflowPhases(Arrays.asList(aWorkflowPhase().deploymentType(KUBERNETES).build()))
            .build();

    assertThat(workflowServiceHelper.workflowHasSshDeploymentPhase(canaryOrchestrationWorkflow_withoutssh)).isFalse();

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow_withoutPhase =
        aCanaryOrchestrationWorkflow().withWorkflowPhases(Collections.emptyList()).build();

    assertThat(workflowServiceHelper.workflowHasSshDeploymentPhase(canaryOrchestrationWorkflow_withoutPhase)).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testObtainDeploymentTypes() {
    List<WorkflowPhase> workflowPhases = ImmutableList.<WorkflowPhase>of(aWorkflowPhase().deploymentType(SSH).build(),
        aWorkflowPhase().deploymentType(KUBERNETES).build(), aWorkflowPhase().deploymentType(ECS).build(),
        aWorkflowPhase().deploymentType(SSH).build(), aWorkflowPhase().deploymentType(ECS).build(),
        aWorkflowPhase().deploymentType(ECS).build(), aWorkflowPhase().build());
    final CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.obtainDeploymentTypes(canaryOrchestrationWorkflow))
        .containsExactlyInAnyOrder(SSH, ECS, KUBERNETES);

    final BuildWorkflow buildWorkflow = BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow().build();
    assertThat(workflowServiceHelper.obtainDeploymentTypes(buildWorkflow)).isEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testNeedArtifactCheckStep() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = null;
    List<WorkflowPhase> workflowPhases = null;
    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping()
            .withAppId(APP_ID)
            .withUuid(INFRA_MAPPING_ID)
            .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
            .build();

    workflowPhases = ImmutableList.<WorkflowPhase>of(aWorkflowPhase().infraMappingId(INFRA_MAPPING_ID).build());

    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, Arrays.asList(INFRA_MAPPING_ID)))
        .thenReturn(Arrays.asList(awsInfrastructureMapping));
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.needArtifactCheckStep(
                   awsInfrastructureMapping.getAppId(), canaryOrchestrationWorkflow, false))
        .isTrue();

    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
            .withAppId(APP_ID)
            .withUuid(INFRA_MAPPING_ID)
            .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
            .build();

    workflowPhases = ImmutableList.<WorkflowPhase>of(aWorkflowPhase().infraMappingId(INFRA_MAPPING_ID).build());

    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, Arrays.asList(INFRA_MAPPING_ID)))
        .thenReturn(Arrays.asList(physicalInfrastructureMapping));
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.needArtifactCheckStep(
                   physicalInfrastructureMapping.getAppId(), canaryOrchestrationWorkflow, false))
        .isTrue();

    PcfInfrastructureMapping pcfInfrastructureMapping = PcfInfrastructureMapping.builder()
                                                            .appId(APP_ID)
                                                            .uuid(INFRA_MAPPING_ID)
                                                            .infraMappingType(InfrastructureMappingType.PCF_PCF.name())
                                                            .build();

    workflowPhases = ImmutableList.<WorkflowPhase>of(aWorkflowPhase().infraMappingId(INFRA_MAPPING_ID).build());

    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, Arrays.asList(INFRA_MAPPING_ID)))
        .thenReturn(Arrays.asList(pcfInfrastructureMapping));
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.needArtifactCheckStep(
                   pcfInfrastructureMapping.getAppId(), canaryOrchestrationWorkflow, false))
        .isTrue();

    GcpKubernetesInfrastructureMapping gcpK8sInfraMapping =
        GcpKubernetesInfrastructureMapping.builder()
            .appId(APP_ID)
            .uuid(INFRA_MAPPING_ID)
            .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.name())
            .build();

    workflowPhases =
        ImmutableList.<WorkflowPhase>of(aWorkflowPhase().infraMappingId(awsInfrastructureMapping.getUuid()).build());

    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, Arrays.asList(INFRA_MAPPING_ID)))
        .thenReturn(Arrays.asList(gcpK8sInfraMapping));
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(
        workflowServiceHelper.needArtifactCheckStep(gcpK8sInfraMapping.getAppId(), canaryOrchestrationWorkflow, false))
        .isFalse();

    workflowPhases = Collections.emptyList();
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.needArtifactCheckStep(APP_ID, canaryOrchestrationWorkflow, false)).isFalse();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .uuid("id1")
                                                            .appId(APP_ID)
                                                            .infrastructure(PcfInfraStructure.builder().build())
                                                            .build();
    when(infrastructureDefinitionService.getInfraStructureDefinitionByUuids(anyString(), anyList()))
        .thenReturn(Arrays.asList(infrastructureDefinition));
    workflowPhases = ImmutableList.<WorkflowPhase>of(aWorkflowPhase().infraDefinitionId(INFRA_DEFINITION_ID).build());
    canaryOrchestrationWorkflow = aCanaryOrchestrationWorkflow().withWorkflowPhases(workflowPhases).build();
    assertThat(workflowServiceHelper.needArtifactCheckStep(APP_ID, canaryOrchestrationWorkflow, true)).isTrue();
    assertThat(workflowServiceHelper.needArtifactCheckStep(APP_ID, canaryOrchestrationWorkflow, false)).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testEnsureArtifactCheckInPreDeployment() {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withPreDeploymentSteps(null).build();

    assertThat(workflowServiceHelper.ensureArtifactCheckInPreDeployment(canaryOrchestrationWorkflow)).isTrue();
    assertThat(workflowServiceHelper.ensureArtifactCheckInPreDeployment(canaryOrchestrationWorkflow)).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testObtainEnvIdWithoutOrchestration() {
    Workflow workflow = aWorkflow().envId(ENV_ID).build();
    assertThat(workflowServiceHelper.obtainEnvIdWithoutOrchestration(workflow, null)).isEqualTo(workflow.getEnvId());

    Workflow workflowTemplated =
        aWorkflow()
            .templateExpressions(singletonList(
                TemplateExpression.builder().fieldName(WorkflowKeys.envId).expression("${envVar}").build()))
            .build();
    assertThat(
        workflowServiceHelper.obtainEnvIdWithoutOrchestration(workflowTemplated, ImmutableMap.of("envVar", ENV_ID)))
        .isEqualTo(ENV_ID);
    assertThat(workflowServiceHelper.obtainEnvIdWithoutOrchestration(
                   workflowTemplated, ImmutableMap.of("envVarOther", ENV_ID)))
        .isNull();
    assertThat(workflowServiceHelper.obtainEnvIdWithoutOrchestration(workflowTemplated, null)).isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testObtainTemplatedEnvironmentId() {
    Workflow workflow = aWorkflow().envId(ENV_ID).build();
    assertThat(workflowServiceHelper.obtainTemplatedEnvironmentId(workflow, null)).isEqualTo(workflow.getEnvId());
    assertThat(workflowServiceHelper.resolveEnvironmentId(workflow, null)).isEqualTo(workflow.getEnvId());

    Workflow workflow_templated =
        aWorkflow()
            .templateExpressions(Arrays.asList(TemplateExpression.builder().fieldName(WorkflowKeys.envId).build()))
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withUserVariables(ImmutableList.of(
                        aVariable().name(WorkflowKeys.envId).entityType(EntityType.ENVIRONMENT).build()))
                    .build())
            .build();
    Map<String, String> workflowVariables = ImmutableMap.of(WorkflowKeys.envId, ENV_ID);
    assertThat(workflowServiceHelper.obtainTemplatedEnvironmentId(workflow_templated, workflowVariables))
        .isEqualTo(ENV_ID);
    assertThat(workflowServiceHelper.obtainTemplatedEnvironmentId(workflow_templated, emptyMap())).isEqualTo(null);

    assertThat(workflowServiceHelper.resolveEnvironmentId(workflow_templated, workflowVariables)).isEqualTo(ENV_ID);
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> workflowServiceHelper.resolveEnvironmentId(workflow_templated, emptyMap()));

    Map<String, String> incorrectVariableMap = ImmutableMap.of(WorkflowKeys.appId, APP_ID);
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> workflowServiceHelper.resolveEnvironmentId(workflow_templated, incorrectVariableMap));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSetGetKeywords() {
    Workflow workflow = aWorkflow()
                            .appId(APP_ID)
                            .envId(ENV_ID)
                            .description("test")
                            .workflowType(WorkflowType.ORCHESTRATION)
                            .name(WORKFLOW_NAME)
                            .notes("test")
                            .build();

    workflowServiceHelper.setKeywords(workflow);
    assertThat(workflow.getKeywords()).isNotEmpty();

    when(environmentService.get(workflow.getAppId(), workflow.getEnvId()))
        .thenReturn(Environment.Builder.anEnvironment().name(ENV_NAME).uuid(ENV_ID).build());

    assertThat(workflowServiceHelper.getKeywords(workflow)).isNotEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateServiceAndInfraDefinition() {
    InfrastructureDefinition infraDef = InfrastructureDefinition.builder()
                                            .uuid(INFRA_DEFINITION_ID)
                                            .appId(APP_ID)
                                            .envId(ENV_ID)
                                            .cloudProviderType(AWS)
                                            .deploymentType(SSH)
                                            .build();
    Service sshService = Service.builder().deploymentType(SSH).uuid(SERVICE_ID).build();
    Service K8sService = Service.builder().deploymentType(KUBERNETES).uuid(SERVICE_ID).build();
    Service noDeploymentTypeService = Service.builder().uuid(SERVICE_ID).build();

    // mock stuff
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(null, noDeploymentTypeService, K8sService, noDeploymentTypeService, sshService);
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(infraDef);

    // service is null
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID));

    // service is noDeploymentTypeService
    workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID);

    // service is K8sService
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID));

    // service is sshService
    workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID);

    // service is sshService
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(null);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID));

    // service is sshService
    infraDef.setScopedToServices(ImmutableList.of(SERVICE_ID + "-test"));
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(infraDef);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID));

    // validate null cases
    workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, SERVICE_ID, null);
    workflowServiceHelper.validateServiceAndInfraDefinition(APP_ID, null, INFRA_DEFINITION_ID);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForSpotinstandOtherAwsAmiWorkflow() {
    InfrastructureDefinition awsAmiInfraDef = InfrastructureDefinition.builder()
                                                  .uuid(INFRA_DEFINITION_ID)
                                                  .appId(APP_ID)
                                                  .envId(ENV_ID)
                                                  .cloudProviderType(AWS)
                                                  .deploymentType(AMI)
                                                  .infrastructure(AwsAmiInfrastructure.builder().build())
                                                  .build();
    Service amiService = Service.builder().deploymentType(AMI).uuid(SERVICE_ID).build();
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .infraMappingId(INFRA_MAPPING_ID)
                                      .infraDefinitionId(INFRA_DEFINITION_ID)
                                      .serviceId(SERVICE_ID)
                                      .build();

    // mock
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(awsAmiInfraDef);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsAmiInfraDef.getInfraMapping());
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(amiService);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSpotinst(APP_ID, workflowPhase, true);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.SPOTINST_SETUP, PhaseStepType.SPOTINST_DEPLOY, VERIFY_SERVICE, WRAP_UP);

    // feature flag on
    workflowPhase.setPhaseSteps(new ArrayList<>());
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSpotinst(APP_ID, workflowPhase, true);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.SPOTINST_SETUP, PhaseStepType.SPOTINST_DEPLOY, VERIFY_SERVICE, WRAP_UP);

    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSpotinst(APP_ID, workflowPhase, false);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes).containsExactly(PhaseStepType.SPOTINST_DEPLOY, VERIFY_SERVICE, WRAP_UP);

    // SPOT INST BLUE GREEN
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSpotInstBlueGreen(APP_ID, workflowPhase, true);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.SPOTINST_SETUP, PhaseStepType.SPOTINST_DEPLOY, VERIFY_SERVICE,
            PhaseStepType.SPOTINST_LISTENER_UPDATE, WRAP_UP);

    // feature flag on
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSpotInstBlueGreen(APP_ID, workflowPhase, true);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.SPOTINST_SETUP, PhaseStepType.SPOTINST_DEPLOY, VERIFY_SERVICE,
            PhaseStepType.SPOTINST_LISTENER_UPDATE, WRAP_UP);

    // AWS AMI BLUE GREEN
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(APP_ID, workflowPhase, true, false);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP, PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP,
            VERIFY_SERVICE, PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, WRAP_UP);

    // feature flag on
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(APP_ID, workflowPhase, true, false);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP, PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP,
            VERIFY_SERVICE, PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, WRAP_UP);

    // dynamic infra
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(APP_ID, workflowPhase, true, true);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PROVISION_INFRASTRUCTURE, PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP,
            PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP, VERIFY_SERVICE,
            PhaseStepType.AMI_SWITCH_AUTOSCALING_GROUP_ROUTES, WRAP_UP);

    // AWS AMI BASIC
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmi(APP_ID, workflowPhase, true, false, BASIC);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP, PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP,
            VERIFY_SERVICE, WRAP_UP);

    // feature flag on
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmi(APP_ID, workflowPhase, true, false, BASIC);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP, PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP,
            VERIFY_SERVICE, WRAP_UP);

    // dynamic infra
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmi(APP_ID, workflowPhase, true, true, BASIC);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PROVISION_INFRASTRUCTURE, PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP,
            PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForAWSLambda() {
    Service lambdaService =
        Service.builder().deploymentType(DeploymentType.AWS_LAMBDA).appId(APP_ID).uuid(SERVICE_ID).build();

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mocks
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(lambdaService);

    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSLambda(APP_ID, workflowPhase);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PREPARE_STEPS, PhaseStepType.DEPLOY_AWS_LAMBDA, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForAWSCodeDeploy() {
    Service codeDeployService =
        Service.builder().deploymentType(DeploymentType.AWS_CODEDEPLOY).uuid(SERVICE_ID).build();

    AmazonS3ArtifactStream artifactStream = AmazonS3ArtifactStream.builder().serviceId(SERVICE_ID).build();

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mocks
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(codeDeployService);
    when(artifactStreamService.fetchArtifactStreamsForService(APP_ID, SERVICE_ID))
        .thenReturn(ImmutableList.of(artifactStream));

    workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSCodeDeploy(APP_ID, workflowPhase);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PREPARE_STEPS, PhaseStepType.DEPLOY_AWSCODEDEPLOY, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECSBlueGreenRoute53() {
    Service ecsService = Service.builder().uuid(SERVICE_ID).appId(APP_ID).deploymentType(ECS).build();
    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mock
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(ecsService);

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECSBlueGreenRoute53(APP_ID, workflowPhase, true);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.CONTAINER_SETUP, PhaseStepType.CONTAINER_DEPLOY, VERIFY_SERVICE,
            PhaseStepType.ECS_UPDATE_ROUTE_53_DNS_WEIGHT, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForPCFBlueGreen() {
    Service pcfService = Service.builder().uuid(SERVICE_ID).appId(APP_ID).deploymentType(DeploymentType.PCF).build();
    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mock
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(pcfService);

    workflowPhase.setDaemonSet(true);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.generateNewWorkflowPhaseStepsForPCFBlueGreen(APP_ID, workflowPhase, true));
    workflowPhase.setDaemonSet(false);

    workflowPhase.setStatefulSet(true);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(
            () -> workflowServiceHelper.generateNewWorkflowPhaseStepsForPCFBlueGreen(APP_ID, workflowPhase, true));
    workflowPhase.setStatefulSet(false);

    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForPCFBlueGreen(APP_ID, workflowPhase, true);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(
            PhaseStepType.PCF_SETUP, PhaseStepType.PCF_RESIZE, VERIFY_SERVICE, PhaseStepType.PCF_SWICH_ROUTES, WRAP_UP);

    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForPCFBlueGreen(APP_ID, workflowPhase, false);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PCF_RESIZE, VERIFY_SERVICE, PhaseStepType.PCF_SWICH_ROUTES, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForPCF() {
    Service pcfService = Service.builder().uuid(SERVICE_ID).appId(APP_ID).deploymentType(DeploymentType.PCF).build();
    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mock
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(pcfService);

    workflowServiceHelper.generateNewWorkflowPhaseStepsForPCF(APP_ID, workflowPhase, true);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.PCF_SETUP, PhaseStepType.PCF_RESIZE, VERIFY_SERVICE, WRAP_UP);

    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForPCF(APP_ID, workflowPhase, false);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes).containsExactly(PhaseStepType.PCF_RESIZE, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForHelm() {
    Service helmService = Service.builder().uuid(SERVICE_ID).appId(APP_ID).deploymentType(HELM).build();

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    // mock
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(helmService);

    workflowServiceHelper.generateNewWorkflowPhaseStepsForHelm(APP_ID, workflowPhase);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes).containsExactly(PhaseStepType.HELM_DEPLOY, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForKubernetes() {
    Service k8sService = Service.builder().appId(APP_ID).uuid(SERVICE_ID).deploymentType(KUBERNETES).build();

    InfrastructureDefinition gcpK8sInfra = InfrastructureDefinition.builder()
                                               .uuid(INFRA_DEFINITION_ID)
                                               .appId(APP_ID)
                                               .envId(ENV_ID)
                                               .cloudProviderType(GCP)
                                               .deploymentType(KUBERNETES)
                                               .infrastructure(GoogleKubernetesEngine.builder()
                                                                   .namespace("namespace")
                                                                   .clusterName("clusterName")
                                                                   .cloudProviderId(SETTING_ID)
                                                                   .build())
                                               .build();

    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .infraMappingId(INFRA_MAPPING_ID)
                                      .infraDefinitionId(INFRA_DEFINITION_ID)
                                      .serviceId(SERVICE_ID)
                                      .build();

    // mock
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(k8sService);
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(gcpK8sInfra);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(gcpK8sInfra.getInfraMapping());
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowServiceHelper.generateNewWorkflowPhaseStepsForKubernetes(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CANARY);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.CONTAINER_SETUP, PhaseStepType.CONTAINER_DEPLOY, VERIFY_SERVICE, WRAP_UP);

    // feature flag on
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForKubernetes(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CANARY);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.CONTAINER_SETUP, PhaseStepType.CONTAINER_DEPLOY, VERIFY_SERVICE, WRAP_UP);

    // ClusterName = RUNTIME
    ((GoogleKubernetesEngine) gcpK8sInfra.getInfrastructure()).setClusterName(WorkflowServiceHelper.RUNTIME);
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForKubernetes(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CANARY);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(
            CLUSTER_SETUP, PhaseStepType.CONTAINER_SETUP, PhaseStepType.CONTAINER_DEPLOY, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForSSH() {
    InfrastructureDefinition sshDefinition = InfrastructureDefinition.builder()
                                                 .uuid(INFRA_DEFINITION_ID)
                                                 .appId(APP_ID)
                                                 .envId(ENV_ID)
                                                 .cloudProviderType(PHYSICAL_DATA_CENTER)
                                                 .deploymentType(SSH)
                                                 .infrastructure(PhysicalInfra.builder().loadBalancerId("lb").build())
                                                 .build();

    Service sshService = Service.builder().appId(APP_ID).uuid(SERVICE_ID).deploymentType(SSH).build();

    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .infraMappingId(INFRA_MAPPING_ID)
                                      .infraDefinitionId(INFRA_DEFINITION_ID)
                                      .serviceId(SERVICE_ID)
                                      .build();

    // mocks
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(sshService);
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(sshDefinition);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(sshDefinition.getInfraMapping());
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, ACCOUNT_ID)).thenReturn(false, true);

    // feature flag off
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSSH(APP_ID, workflowPhase, OrchestrationWorkflowType.CANARY);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.INFRASTRUCTURE_NODE, PhaseStepType.DISABLE_SERVICE, DEPLOY_SERVICE,
            ENABLE_SERVICE, VERIFY_SERVICE, WRAP_UP);

    // feature flag on
    workflowPhase.getPhaseSteps().clear();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForSSH(APP_ID, workflowPhase, OrchestrationWorkflowType.CANARY);
    phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes)
        .containsExactly(PhaseStepType.INFRASTRUCTURE_NODE, PhaseStepType.DISABLE_SERVICE, DEPLOY_SERVICE,
            ENABLE_SERVICE, VERIFY_SERVICE, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForArtifactCollection() {
    WorkflowPhase workflowPhase = aWorkflowPhase().build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForArtifactCollection(workflowPhase);
    List<PhaseStepType> phaseStepTypes =
        workflowPhase.getPhaseSteps().stream().map(PhaseStep::getPhaseStepType).collect(Collectors.toList());
    assertThat(phaseStepTypes).containsExactly(PhaseStepType.PREPARE_STEPS, PhaseStepType.COLLECT_ARTIFACT, WRAP_UP);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testIsK8sV2Service() {
    Service k8sService = Service.builder().isK8sV2(true).build();

    // mock
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(k8sService);

    assertThat(workflowServiceHelper.isK8sV2Service(APP_ID, SERVICE_ID)).isTrue();
    k8sService.setK8sV2(false);

    assertThat(workflowServiceHelper.isK8sV2Service(APP_ID, SERVICE_ID)).isFalse();
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupEmptyStepSkipStrategies() {
    // null phase step.
    WorkflowServiceHelper.cleanupStepSkipStrategies(null);
    // empty step skip strategies.
    WorkflowServiceHelper.cleanupStepSkipStrategies(aPhaseStep(PRE_DEPLOYMENT).build());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupStepSkipStrategies() {
    StepSkipStrategy strategy1 = new StepSkipStrategy(SPECIFIC_STEPS, singletonList("id1"), "true");
    StepSkipStrategy strategy2 = new StepSkipStrategy(SPECIFIC_STEPS, singletonList("id2"), "true");
    StepSkipStrategy strategy3 = new StepSkipStrategy(SPECIFIC_STEPS, asList("id1", "id2"), "true");
    PhaseStep phaseStep = aPhaseStep(PRE_DEPLOYMENT).withStepSkipStrategies(singletonList(strategy1)).build();

    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    assertThat(phaseStep.getStepSkipStrategies()).isNullOrEmpty();

    StepSkipStrategy strategy4 = new StepSkipStrategy(ALL_STEPS, null, "true");
    phaseStep.setStepSkipStrategies(singletonList(strategy4));
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    assertThat(phaseStep.getStepSkipStrategies()).isNotEmpty();

    phaseStep.setSteps(asList(prepareGraphNode(2), prepareGraphNode(3)));
    phaseStep.setStepSkipStrategies(singletonList(strategy1));
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    assertThat(phaseStep.getStepSkipStrategies()).isNullOrEmpty();

    phaseStep.setStepSkipStrategies(singletonList(strategy2));
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    assertThat(phaseStep.getStepSkipStrategies()).isNotEmpty();

    phaseStep.setStepSkipStrategies(singletonList(strategy3));
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    assertThat(phaseStep.getStepSkipStrategies()).isNotEmpty();
    assertThat(phaseStep.getStepSkipStrategies().size()).isEqualTo(1);

    StepSkipStrategy finalStrategy = phaseStep.getStepSkipStrategies().get(0);
    assertThat(finalStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(finalStrategy.getStepIds()).containsExactly("id2");
  }

  private GraphNode prepareGraphNode(int idx) {
    return GraphNode.builder().id("id" + idx).build();
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupEmptyPhaseStepSkipStrategies() {
    // null phase.
    WorkflowServiceHelper.cleanupPhaseStepSkipStrategies(null);
    // empty step skip strategies.
    WorkflowServiceHelper.cleanupPhaseStepSkipStrategies(aWorkflowPhase().build());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupPhaseStepSkipStrategies() {
    PhaseStep phaseStep =
        aPhaseStep(PRE_DEPLOYMENT)
            .addStep(prepareGraphNode(2))
            .addStep(prepareGraphNode(3))
            .withStepSkipStrategies(singletonList(new StepSkipStrategy(SPECIFIC_STEPS, asList("id1", "id2"), "true")))
            .build();
    WorkflowPhase phase = aWorkflowPhase().phaseSteps(singletonList(phaseStep)).build();
    WorkflowServiceHelper.cleanupPhaseStepSkipStrategies(phase);
    assertThat(phaseStep.getStepSkipStrategies()).isNotEmpty();

    StepSkipStrategy finalStrategy = phaseStep.getStepSkipStrategies().get(0);
    assertThat(finalStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(finalStrategy.getStepIds()).containsExactly("id2");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupEmptyWorkflowStepSkipStrategies() {
    // null phase.
    WorkflowServiceHelper.cleanupWorkflowStepSkipStrategies(null);
    // empty step skip strategies.
    WorkflowServiceHelper.cleanupWorkflowStepSkipStrategies(aCanaryOrchestrationWorkflow().build());
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCleanupWorkflowStepSkipStrategies() {
    PhaseStep phaseStep =
        aPhaseStep(PRE_DEPLOYMENT)
            .addStep(prepareGraphNode(2))
            .addStep(prepareGraphNode(3))
            .withStepSkipStrategies(singletonList(new StepSkipStrategy(SPECIFIC_STEPS, asList("id1", "id2"), "true")))
            .build();
    WorkflowPhase phase = aWorkflowPhase().phaseSteps(singletonList(phaseStep)).build();
    OrchestrationWorkflow orchestrationWorkflow = aCanaryOrchestrationWorkflow().addWorkflowPhase(phase).build();
    WorkflowServiceHelper.cleanupWorkflowStepSkipStrategies(orchestrationWorkflow);
    assertThat(phaseStep.getStepSkipStrategies()).isNotEmpty();

    StepSkipStrategy finalStrategy = phaseStep.getStepSkipStrategies().get(0);
    assertThat(finalStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(finalStrategy.getStepIds()).containsExactly("id2");
  }

  private void verifyPhase(WorkflowPhase workflowPhase, List<String> expectedNames, int stepCount) {
    List<PhaseStep> phaseStepList = workflowPhase.getPhaseSteps();
    assertThat(phaseStepList).isNotNull();
    assertThat(phaseStepList).hasSize(stepCount);

    for (int index = 0; index < expectedNames.size(); index++) {
      if (EmptyPredicate.isNotEmpty(expectedNames.get(index))) {
        PhaseStep phaseStep = phaseStepList.get(index);
        assertThat(phaseStep.getSteps()).isNotNull();
        assertThat(phaseStep.getSteps()).hasSize(1);
        assertThat(phaseStep.getSteps().get(0).getType()).isEqualTo(expectedNames.get(index));
      }
    }
  }
}
