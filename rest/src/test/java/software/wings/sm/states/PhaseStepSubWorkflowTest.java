package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.EcsServiceElement.EcsServiceElementBuilder.anEcsServiceElement;
import static software.wings.api.KubernetesReplicationControllerElement.KubernetesReplicationControllerElementBuilder.aKubernetesReplicationControllerElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.ElementNotifyResponseData.Builder.anElementNotifyResponseData;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PHASE_STEP;

import com.google.common.collect.Lists;

import org.joor.Reflect;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.EcsServiceElement;
import software.wings.api.KubernetesReplicationControllerElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepSubWorkflowExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ErrorCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStepType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ServiceInstancesProvisionState;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 2/25/17.
 */
public class PhaseStepSubWorkflowTest extends WingsBaseTest {
  private static final String STATE_NAME = "state";
  @Mock private WorkflowExecutionService workflowExecutionService;

  private List<ElementExecutionSummary> elementExecutionSummaries = new ArrayList<>();

  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams().withAppId(APP_ID).build();

  @Test
  public void shouldExecutePreDeployStep() {
    PhaseElement phaseElement =
        aPhaseElement()
            .withUuid(getUuid())
            .withServiceElement(aServiceElement().withUuid(getUuid()).withName("service1").build())
            .build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.PRE_DEPLOYMENT);
    List<FailureStrategy> failureStrategies = new ArrayList<>();
    phaseStepSubWorkflow.setFailureStrategies(failureStrategies);
    phaseStepSubWorkflow.setStepsInParallel(true);
    phaseStepSubWorkflow.setDefaultFailureStrategy(true);
    ExecutionResponse response = phaseStepSubWorkflow.execute(context);
    assertThat(response).isNotNull().hasFieldOrProperty("stateExecutionData");
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .hasFieldOrPropertyWithValue("failureStrategies", failureStrategies)
        .hasFieldOrPropertyWithValue("defaultFailureStrategy", true)
        .hasFieldOrPropertyWithValue("stepsInParallel", true);
  }

  @Test
  public void shouldThrowInvalidRequestNoEcsSetup() {
    try {
      PhaseElement phaseElement =
          aPhaseElement()
              .withUuid(getUuid())
              .withServiceElement(aServiceElement().withUuid(getUuid()).withName("service1").build())
              .build();
      StateExecutionInstance stateExecutionInstance =
          aStateExecutionInstance()
              .withStateName(STATE_NAME)
              .addContextElement(workflowStandardParams)
              .addContextElement(phaseElement)
              .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
              .build();
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
      PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
      phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_DEPLOY);
      ExecutionResponse response = phaseStepSubWorkflow.execute(context);
      assertThat(response).isNotNull();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Setup not done");
    }
  }

  @Test
  public void shouldThrowInvalidRequestNoEcsElement() {
    try {
      PhaseElement phaseElement =
          aPhaseElement()
              .withUuid(getUuid())
              .withServiceElement(aServiceElement().withUuid(getUuid()).withName("service1").build())
              .build();
      StateExecutionInstance stateExecutionInstance =
          aStateExecutionInstance()
              .withStateName(STATE_NAME)
              .addContextElement(workflowStandardParams)
              .addContextElement(phaseElement)
              .addContextElement(anEcsServiceElement().build())
              .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
              .build();
      ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
      PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
      phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_DEPLOY);
      ExecutionResponse response = phaseStepSubWorkflow.execute(context);
      assertThat(response).isNotNull();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message"))
          .asString()
          .contains("ecsServiceElement not present for the service");
    }
  }

  @Test
  public void shouldValidateContainerDeploy() {
    ServiceElement serviceElement = aServiceElement().withUuid(getUuid()).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement().withUuid(getUuid()).withServiceElement(serviceElement).build();
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance()
            .withStateName(STATE_NAME)
            .addContextElement(workflowStandardParams)
            .addContextElement(phaseElement)
            .addContextElement(anEcsServiceElement().withUuid(serviceElement.getUuid()).build())
            .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
            .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_DEPLOY);
    ExecutionResponse response = phaseStepSubWorkflow.execute(context);
    assertThat(response).isNotNull().hasFieldOrProperty("stateExecutionData");
  }

  @Test
  public void shouldThrowNullPhaseType() {
    try {
      ExecutionContextImpl context = new ExecutionContextImpl(null);
      PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
      ExecutionResponse response = phaseStepSubWorkflow.execute(context);
      assertThat(response).isNotNull();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("null phaseStepType");
    }
  }

  @Test
  public void shouldHandleAsyncPreDeploy() {
    ExecutionContextImpl context = new ExecutionContextImpl(null);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.PRE_DEPLOYMENT);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    ExecutionResponse response = phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    assertThat(response).isNotNull();
  }

  @Test
  public void shouldHandleAsyncProvisionNode() {
    when(workflowExecutionService.getElementsSummary(anyString(), anyString(), anyString()))
        .thenReturn(elementExecutionSummaries);

    List<String> instanceIds = Lists.newArrayList(getUuid(), getUuid());
    String serviceId = getUuid();
    ServiceInstanceIdsParam serviceInstanceIdsParam =
        ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam()
            .withInstanceIds(instanceIds)
            .withServiceId(serviceId)
            .build();

    ServiceElement serviceElement = aServiceElement().withUuid(serviceId).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getUuid())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(DeploymentType.SSH.name())
                                    .build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.PROVISION_NODE);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", anElementNotifyResponseData().addContextElement(serviceInstanceIdsParam).build());
    Reflect.on(phaseStepSubWorkflow).set("workflowExecutionService", workflowExecutionService);

    ExecutionResponse response = phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    assertThat(response).isNotNull().hasFieldOrProperty("stateExecutionData");
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isExactlyInstanceOf(PhaseStepSubWorkflowExecutionData.class);
    PhaseStepSubWorkflowExecutionData phaseStepSubWorkflowExecutionData =
        (PhaseStepSubWorkflowExecutionData) response.getStateExecutionData();
    assertThat(phaseStepSubWorkflowExecutionData.getPhaseStepExecutionState())
        .isNotNull()
        .isExactlyInstanceOf(ServiceInstancesProvisionState.class);
    ServiceInstancesProvisionState serviceInstancesProvisionState =
        (ServiceInstancesProvisionState) phaseStepSubWorkflowExecutionData.getPhaseStepExecutionState();
    assertThat(serviceInstancesProvisionState.getInstanceIds()).isNotNull().isEqualTo(instanceIds);

    assertThat(response.getContextElements()).isNotNull().hasSize(1);
    assertThat(response.getContextElements().get(0)).isNotNull().isEqualTo(serviceInstanceIdsParam);
  }

  @Test
  public void shouldHandleAsyncEcsSetup() {
    when(workflowExecutionService.getElementsSummary(anyString(), anyString(), anyString()))
        .thenReturn(elementExecutionSummaries);

    String serviceId = getUuid();
    ServiceElement serviceElement = aServiceElement().withUuid(serviceId).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getUuid())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(DeploymentType.ECS.name())
                                    .build();

    EcsServiceElement ecsServiceElement = anEcsServiceElement().withUuid(serviceElement.getUuid()).build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_SETUP);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", anElementNotifyResponseData().addContextElement(ecsServiceElement).build());
    Reflect.on(phaseStepSubWorkflow).set("workflowExecutionService", workflowExecutionService);

    ExecutionResponse response = phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    assertThat(response).isNotNull().hasFieldOrProperty("stateExecutionData");
    assertThat(response.getContextElements()).isNotNull().hasSize(1);
    assertThat(response.getContextElements().get(0)).isNotNull().isEqualTo(ecsServiceElement);
  }

  @Test
  public void shouldThrowInvalidEcsSetup() {
    when(workflowExecutionService.getElementsSummary(anyString(), anyString(), anyString()))
        .thenReturn(elementExecutionSummaries);

    String serviceId = getUuid();
    ServiceElement serviceElement = aServiceElement().withUuid(serviceId).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getUuid())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(DeploymentType.ECS.name())
                                    .build();

    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_SETUP);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    try {
      phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Missing ECSServiceElement");
    }
  }

  @Test
  public void shouldHandleAsyncKubernetesSetup() {
    when(workflowExecutionService.getElementsSummary(anyString(), anyString(), anyString()))
        .thenReturn(elementExecutionSummaries);

    String serviceId = getUuid();
    ServiceElement serviceElement = aServiceElement().withUuid(serviceId).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getUuid())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(DeploymentType.KUBERNETES.name())
                                    .build();

    KubernetesReplicationControllerElement ecsServiceElement =
        aKubernetesReplicationControllerElement().withUuid(serviceElement.getUuid()).build();
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_SETUP);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    notifyResponse.put("key", anElementNotifyResponseData().addContextElement(ecsServiceElement).build());
    Reflect.on(phaseStepSubWorkflow).set("workflowExecutionService", workflowExecutionService);

    ExecutionResponse response = phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    assertThat(response).isNotNull().hasFieldOrProperty("stateExecutionData");
    assertThat(response.getContextElements()).isNotNull().hasSize(1);
    assertThat(response.getContextElements().get(0)).isNotNull().isEqualTo(ecsServiceElement);
  }

  @Test
  public void shouldThrowInvalidKubernetesSetup() {
    when(workflowExecutionService.getElementsSummary(anyString(), anyString(), anyString()))
        .thenReturn(elementExecutionSummaries);

    String serviceId = getUuid();
    ServiceElement serviceElement = aServiceElement().withUuid(serviceId).withName("service1").build();
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getUuid())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(DeploymentType.KUBERNETES.name())
                                    .build();

    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withStateName(STATE_NAME)
                                                        .addContextElement(workflowStandardParams)
                                                        .addContextElement(phaseElement)
                                                        .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
                                                        .build();
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow(PHASE_STEP);
    phaseStepSubWorkflow.setPhaseStepType(PhaseStepType.CONTAINER_SETUP);
    Map<String, NotifyResponseData> notifyResponse = new HashMap<>();
    try {
      phaseStepSubWorkflow.handleAsyncResponse(context, notifyResponse);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message"))
          .asString()
          .contains("Missing KubernetesReplicationControllerElement");
    }
  }
}
