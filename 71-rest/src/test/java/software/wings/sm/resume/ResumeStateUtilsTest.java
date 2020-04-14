package software.wings.sm.resume;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.WorkflowElement;
import software.wings.api.artifact.ServiceArtifactElement;
import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.impl.SweepingOutputServiceImplTest;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ResumeStateUtilsTest extends WingsBaseTest {
  private static final String SWEEPING_OUTPUT_CONTENT = "SWEEPING_OUTPUT_CONTENT";

  @Inject @InjectMocks private ResumeStateUtils resumeStateUtils;
  @Inject private SweepingOutputService sweepingOutputService;

  @Mock private StateExecutionService stateExecutionService;

  private final String appId = generateUuid();
  private final String workflowExecutionUuid = generateUuid();
  private final String pipelineExecutionUuid = generateUuid();
  private final String phaseElementId = generateUuid();

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldPrepareExecutionResponse() {
    String prevStateExecutionId = "id";
    String errMsg = "errMsg";
    LinkedList<ContextElement> contextElements = new LinkedList<>(asList(HostElement.builder().build(),
        ServiceArtifactElement.builder().build(), ServiceArtifactVariableElement.builder().build()));
    StateExecutionInstance stateExecutionInstance = mock(StateExecutionInstance.class);
    when(stateExecutionInstance.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    when(stateExecutionInstance.getContextElements()).thenReturn(contextElements);
    StateExecutionData stateExecutionData = aStateExecutionData().withErrorMsg(errMsg).build();
    when(stateExecutionInstance.fetchStateExecutionData()).thenReturn(stateExecutionData);
    when(stateExecutionService.getStateExecutionData(eq(APP_ID), eq(prevStateExecutionId)))
        .thenReturn(stateExecutionInstance);

    ExecutionContext context = mock(ExecutionContextImpl.class);
    when(context.getAppId()).thenReturn(APP_ID);
    ExecutionResponse executionResponse = resumeStateUtils.prepareExecutionResponse(context, prevStateExecutionId);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getErrorMessage()).isEqualTo(errMsg);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(stateExecutionData);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(stateExecutionData);
    assertThat(executionResponse.getContextElements()).isNotNull();
    assertThat(executionResponse.getContextElements().size()).isEqualTo(2);
    assertThat(executionResponse.getContextElements()
                   .stream()
                   .map(ContextElement::getElementType)
                   .collect(Collectors.toList()))
        .containsExactly(ContextElementType.ARTIFACT, ContextElementType.ARTIFACT_VARIABLE);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFetchPipelineExecutionId() {
    ExecutionContext context = mock(ExecutionContextImpl.class);
    when(context.getContextElement(eq(ContextElementType.STANDARD))).thenReturn(null);
    assertThat(resumeStateUtils.fetchPipelineExecutionId(context)).isNull();

    WorkflowStandardParams std = mock(WorkflowStandardParams.class);
    when(context.getContextElement(eq(ContextElementType.STANDARD))).thenReturn(std);
    when(std.getWorkflowElement()).thenReturn(null);
    assertThat(resumeStateUtils.fetchPipelineExecutionId(context)).isNull();

    String pipelineExecutionId = "id";
    when(std.getWorkflowElement())
        .thenReturn(WorkflowElement.builder().pipelineDeploymentUuid(pipelineExecutionId).build());
    assertThat(resumeStateUtils.fetchPipelineExecutionId(context)).isEqualTo(pipelineExecutionId);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCopyPipelineStageOutputs() {
    // Setup some existing sweeping outputs.
    String stateExecutionInstanceIdEnvState = generateUuid();
    String stateExecutionInstanceIdApprovalState = generateUuid();
    String phaseExecutionId = workflowExecutionUuid + phaseElementId + "Phase 1";

    // This should not be copied as it's at the workflow scope.
    sweepingOutputService.save(
        SweepingOutputServiceImpl
            .prepareSweepingOutputBuilder(appId, pipelineExecutionUuid, workflowExecutionUuid, phaseExecutionId,
                stateExecutionInstanceIdEnvState, SweepingOutputInstance.Scope.WORKFLOW)
            .name("wf1")
            .output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT))
            .value(SweepingOutputServiceImplTest.SweepingOutputData.builder().text(SWEEPING_OUTPUT_CONTENT).build())
            .build());

    // All the others should be copied.
    sweepingOutputService.save(
        SweepingOutputServiceImpl
            .prepareSweepingOutputBuilder(appId, pipelineExecutionUuid, workflowExecutionUuid, phaseExecutionId,
                stateExecutionInstanceIdEnvState, SweepingOutputInstance.Scope.PIPELINE)
            .name("pl1")
            .output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT))
            .value(SweepingOutputServiceImplTest.SweepingOutputData.builder().text(SWEEPING_OUTPUT_CONTENT).build())
            .build());
    sweepingOutputService.save(
        SweepingOutputServiceImpl
            .prepareSweepingOutputBuilder(appId, pipelineExecutionUuid, workflowExecutionUuid, phaseExecutionId,
                stateExecutionInstanceIdEnvState, SweepingOutputInstance.Scope.PIPELINE)
            .name("pl2")
            .output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT))
            .value(SweepingOutputServiceImplTest.SweepingOutputData.builder().text(SWEEPING_OUTPUT_CONTENT).build())
            .build());
    sweepingOutputService.save(
        SweepingOutputServiceImpl
            .prepareSweepingOutputBuilder(appId, pipelineExecutionUuid, null, null,
                stateExecutionInstanceIdApprovalState, SweepingOutputInstance.Scope.PIPELINE)
            .name("pl3")
            .output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT))
            .value(SweepingOutputServiceImplTest.SweepingOutputData.builder().text(SWEEPING_OUTPUT_CONTENT).build())
            .build());

    String newPipelineExecutionUuid = generateUuid();
    String newStateExecutionInstanceIdEnvState = generateUuid();
    String newStateExecutionInstanceIdApprovalState = generateUuid();

    // This should be a no-op as the pipeline execution ids are the same.
    resumeStateUtils.copyPipelineStageOutputs(appId, newPipelineExecutionUuid, stateExecutionInstanceIdEnvState,
        singletonList(workflowExecutionUuid), newPipelineExecutionUuid, newStateExecutionInstanceIdEnvState);
    List<SweepingOutputInstance> instances = listPipelineInstances(newPipelineExecutionUuid);
    assertThat(instances).isNullOrEmpty();

    // This should be a no-op as the old pipeline execution id has no outputs.
    resumeStateUtils.copyPipelineStageOutputs(appId, pipelineExecutionUuid + "random", stateExecutionInstanceIdEnvState,
        singletonList(workflowExecutionUuid), newPipelineExecutionUuid, newStateExecutionInstanceIdEnvState);
    instances = listPipelineInstances(newPipelineExecutionUuid);
    assertThat(instances).isNullOrEmpty();

    // Copy outputs.
    resumeStateUtils.copyPipelineStageOutputs(appId, pipelineExecutionUuid, stateExecutionInstanceIdEnvState,
        singletonList(workflowExecutionUuid), newPipelineExecutionUuid, newStateExecutionInstanceIdEnvState);
    resumeStateUtils.copyPipelineStageOutputs(appId, pipelineExecutionUuid, stateExecutionInstanceIdApprovalState, null,
        newPipelineExecutionUuid, newStateExecutionInstanceIdApprovalState);

    instances = listPipelineInstances(newPipelineExecutionUuid);
    assertThat(instances).isNotNull();
    assertThat(instances.size()).isEqualTo(3);

    SweepingOutputInstance instance = getInstanceByName(instances, "wf1");
    assertThat(instance).isNull();

    instance = getInstanceByName(instances, "pl1");
    assertThat(instance).isNotNull();
    assertThat(instance.getWorkflowExecutionIds()).containsExactly(newPipelineExecutionUuid);
    assertThat(instance.getStateExecutionId()).isEqualTo(newStateExecutionInstanceIdEnvState);
    verifyOutputAndValue(instance);

    instance = getInstanceByName(instances, "pl2");
    assertThat(instance).isNotNull();
    assertThat(instance.getWorkflowExecutionIds()).containsExactly(newPipelineExecutionUuid);
    assertThat(instance.getStateExecutionId()).isEqualTo(newStateExecutionInstanceIdEnvState);
    verifyOutputAndValue(instance);

    instance = getInstanceByName(instances, "pl3");
    assertThat(instance).isNotNull();
    assertThat(instance.getWorkflowExecutionIds()).containsExactly(newPipelineExecutionUuid);
    assertThat(instance.getStateExecutionId()).isEqualTo(newStateExecutionInstanceIdApprovalState);
    verifyOutputAndValue(instance);
  }

  private List<SweepingOutputInstance> listPipelineInstances(String pipelineExecutionUuid) {
    return wingsPersistence.createQuery(SweepingOutputInstance.class)
        .filter(SweepingOutputInstance.SweepingOutputKeys.appId, appId)
        .filter(SweepingOutputInstance.SweepingOutputKeys.pipelineExecutionId, pipelineExecutionUuid)
        .asList();
  }

  private SweepingOutputInstance getInstanceByName(List<SweepingOutputInstance> instances, String name) {
    return instances.stream().filter(instance -> instance.getName().equals(name)).findFirst().orElse(null);
  }

  private void verifyOutputAndValue(SweepingOutputInstance instance) {
    assertThat(instance.getOutput()).isEqualTo(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT));
    assertThat(((SweepingOutputServiceImplTest.SweepingOutputData) instance.getValue()).getText())
        .isEqualTo(SWEEPING_OUTPUT_CONTENT);
  }
}
