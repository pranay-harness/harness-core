package software.wings.service.impl.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.ECS_DAEMON_SCHEDULING_STRATEGY;
import static software.wings.common.Constants.ECS_REPLICA_SCHEDULING_STRATEGY;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_LISTENER_UPDATE;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP_ROLLBACK;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.EmptyPredicate;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowServiceHelperTest extends WingsBaseTest {
  private static String envId = generateUuid();

  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceHelperTest.class);
  @Mock private ServiceResourceService serviceResourceService;
  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Test
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

    assertEquals(yamlForHPAWithCustomMetric, hpaString);
  }

  @Test
  @Category(UnitTests.class)
  public void testIsDaemonSchedulingStrategy() throws Exception {
    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    boolean isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertTrue(isDaemonSchedulingStrategy);

    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.CANARY);
    assertFalse(isDaemonSchedulingStrategy);
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.ROLLING);
    assertFalse(isDaemonSchedulingStrategy);
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.CUSTOM);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.resetToDefaultSpecification();
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    isDaemonSchedulingStrategy = workflowServiceHelper.isDaemonSchedulingStrategy(
        APP_ID, aWorkflowPhase().serviceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertTrue(isDaemonSchedulingStrategy);
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification specification = EcsServiceSpecification.builder().build();
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(specification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    specification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
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
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_BG() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

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
  @Category(UnitTests.class)
  public void testGenerateNewWorkflowPhaseStepsForECS_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase = workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(
        APP_ID, workflowPhase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.BASIC);
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
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_BG() {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase = workflowServiceHelper.generateRollbackWorkflowPhaseForEcsBlueGreen(
        APP_ID, workflowPhase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase,
        asList(new String[] {StateType.ECS_LISTENER_UPDATE_ROLLBACK.name(), ECS_SERVICE_ROLLBACK.name()}), 4);
  }

  @Test
  @Category(UnitTests.class)
  public void testGenerateRollbackWorkflowPhaseForEcs_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification ecsServiceSpecification = EcsServiceSpecification.builder().build();
    ecsServiceSpecification.setServiceSpecJson(null);
    ecsServiceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(ecsServiceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase phase = aWorkflowPhase().serviceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);

    ecsServiceSpecification.resetToDefaultSpecification();
    ecsServiceSpecification.setServiceSpecJson(
        ecsServiceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    phase = aWorkflowPhase().serviceId(SERVICE_ID).build();
    rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);
  }

  @Test
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

  private void verifyPhase(WorkflowPhase workflowPhase, List<String> expectedNames, int stepCount) {
    List<PhaseStep> phaseStepList = workflowPhase.getPhaseSteps();
    assertNotNull(phaseStepList);
    assertEquals(stepCount, phaseStepList.size());

    for (int index = 0; index < expectedNames.size(); index++) {
      if (EmptyPredicate.isNotEmpty(expectedNames.get(index))) {
        PhaseStep phaseStep = phaseStepList.get(index);
        assertNotNull(phaseStep.getSteps());
        assertEquals(1, phaseStep.getSteps().size());
        assertEquals(expectedNames.get(index), phaseStep.getSteps().get(0).getType());
      }
    }
  }
}
