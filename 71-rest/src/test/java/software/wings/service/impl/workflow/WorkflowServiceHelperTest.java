package software.wings.service.impl.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.ECS_DAEMON_SCHEDULING_STRATEGY;
import static software.wings.common.Constants.ECS_REPLICA_SCHEDULING_STRATEGY;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP_ROLLBACK;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.PhaseStep;
import software.wings.beans.Service;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Arrays;
import java.util.List;

public class WorkflowServiceHelperTest extends WingsBaseTest {
  private static String envId = generateUuid();

  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceHelperTest.class);
  @Mock private ServiceResourceService serviceResourceService;
  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Test
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
  public void testIsDaemonSchedulingStrategy() throws Exception {
    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    boolean isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertTrue(isDaemonSchedulingStrategy);

    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.CANARY);
    assertFalse(isDaemonSchedulingStrategy);
    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.ROLLING);
    assertFalse(isDaemonSchedulingStrategy);
    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.CUSTOM);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.resetToDefaultSpecification();
    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertFalse(isDaemonSchedulingStrategy);

    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    isDaemonSchedulingStrategy =
        (boolean) MethodUtils.invokeMethod(workflowServiceHelper, true, "isDaemonSchedulingStrategy", APP_ID,
            aWorkflowPhase().withServiceId(SERVICE_ID).build(), OrchestrationWorkflowType.BASIC);
    assertTrue(isDaemonSchedulingStrategy);
  }

  @Test
  public void testGenerateNewWorkflowPhaseStepsForECS_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification specification = EcsServiceSpecification.builder().build();
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(specification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    specification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    // Non basic workflows should not respect DEAMON scheduling strategy
    specification.setServiceSpecJson(null);
    specification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CANARY);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);

    specification.resetToDefaultSpecification();
    specification.setServiceSpecJson(specification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.CUSTOM);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP.name(), ECS_SERVICE_DEPLOY.name()}), 4);
  }

  @Test
  public void testGenerateNewWorkflowPhaseStepsForECS_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();

    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
        APP_ID, workflowPhase, true, OrchestrationWorkflowType.BASIC);
    verifyPhase(workflowPhase, Arrays.asList(new String[] {ECS_DAEMON_SERVICE_SETUP.name()}), 3);
  }
  @Test
  public void testGenerateRollbackWorkflowPhaseForEcs_ReplicaStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification serviceSpecification = EcsServiceSpecification.builder().build();
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_REPLICA_SCHEDULING_STRATEGY);
    doReturn(serviceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase = workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(
        APP_ID, workflowPhase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    // Non basic workflows should not respect DEAMON scheduling strategy
    serviceSpecification.setServiceSpecJson(null);
    serviceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.CANARY);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);

    serviceSpecification.resetToDefaultSpecification();
    serviceSpecification.setServiceSpecJson(serviceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    workflowPhase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, workflowPhase, OrchestrationWorkflowType.CUSTOM);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_ROLLBACK.name()}), 3);
  }

  @Test
  public void testGenerateRollbackWorkflowPhaseForEcs_DaemonStrategy() throws Exception {
    doReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(null).build())
        .when(serviceResourceService)
        .get(anyString(), anyString());

    EcsServiceSpecification ecsServiceSpecification = EcsServiceSpecification.builder().build();
    ecsServiceSpecification.setServiceSpecJson(null);
    ecsServiceSpecification.setSchedulingStrategy(ECS_DAEMON_SCHEDULING_STRATEGY);
    doReturn(ecsServiceSpecification).when(serviceResourceService).getEcsServiceSpecification(anyString(), anyString());

    WorkflowPhase phase = aWorkflowPhase().withServiceId(SERVICE_ID).build();

    WorkflowPhase rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);

    ecsServiceSpecification.resetToDefaultSpecification();
    ecsServiceSpecification.setServiceSpecJson(
        ecsServiceSpecification.getServiceSpecJson().replace("REPLICA", "DAEMON"));
    phase = aWorkflowPhase().withServiceId(SERVICE_ID).build();
    rollbackPhase =
        workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(APP_ID, phase, OrchestrationWorkflowType.BASIC);
    verifyPhase(rollbackPhase, Arrays.asList(new String[] {ECS_SERVICE_SETUP_ROLLBACK.name()}), 3);
  }

  private void verifyPhase(WorkflowPhase workflowPhase, List<String> expectedNames, int stepCount) {
    List<PhaseStep> phaseStepList = workflowPhase.getPhaseSteps();
    assertNotNull(phaseStepList);
    assertEquals(stepCount, phaseStepList.size());

    for (int index = 0; index < expectedNames.size(); index++) {
      PhaseStep phaseStep = phaseStepList.get(index);
      assertNotNull(phaseStep.getSteps());
      assertEquals(1, phaseStep.getSteps().size());
      assertEquals(expectedNames.get(index), phaseStep.getSteps().get(0).getType());
    }
  }
}
