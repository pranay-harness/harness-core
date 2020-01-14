package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.LinkedList;

public class SweepingOutputServiceImplTest extends WingsBaseTest {
  private static final String SWEEPING_OUTPUT_NAME = "SWEEPING_OUTPUT_NAME";
  private static final String SWEEPING_OUTPUT_CONTENT = "SWEEPING_OUTPUT_CONTENT";

  private final String infraDefinitionId = generateUuid();
  private final String workflowExecutionUuid = generateUuid();
  private final String pipelineExecutionUuid = generateUuid();
  private final String appId = generateUuid();
  private final String stateExecutionInstanceId = generateUuid();
  private final String phaseElementId = generateUuid();
  private final String phaseName = "Phase 1";

  @InjectMocks @Inject private SweepingOutputService sweepingOutputService;

  private SweepingOutputInstance sweepingOutputInstance;
  private StateExecutionInstance stateExecutionInstance;

  @Value
  @Builder
  public static class SweepingOutputData implements SweepingOutput {
    String text;
  }

  @Before
  public void setup() {
    LinkedList<ContextElement> contextElements = new LinkedList<>();
    ContextElement phaseElement = PhaseElement.builder()
                                      .uuid(phaseElementId)
                                      .infraDefinitionId(infraDefinitionId)
                                      .rollback(false)
                                      .phaseName(phaseName)
                                      .phaseNameForRollback("Rollback Phase 1")
                                      .onDemandRollback(false)
                                      .build();
    contextElements.add(phaseElement);
    stateExecutionInstance = aStateExecutionInstance()
                                 .uuid(stateExecutionInstanceId)
                                 .appId(appId)
                                 .executionUuid(workflowExecutionUuid)
                                 .stateType(StateType.AWS_NODE_SELECT.name())
                                 .displayName(StateType.AWS_NODE_SELECT.name())
                                 .stateName(StateType.AWS_NODE_SELECT.name())
                                 .contextElements(contextElements)
                                 .build();
    String phaseExecutionId = workflowExecutionUuid + phaseElementId + "Phase 1";
    SweepingOutputInstanceBuilder sweepingOutputBuilder = SweepingOutputServiceImpl.prepareSweepingOutputBuilder(appId,
        pipelineExecutionUuid, workflowExecutionUuid, phaseExecutionId, stateExecutionInstanceId, Scope.WORKFLOW);

    sweepingOutputInstance =
        sweepingOutputService.save(sweepingOutputBuilder.name(SWEEPING_OUTPUT_NAME)
                                       .output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT))
                                       .value(SweepingOutputData.builder().text(SWEEPING_OUTPUT_CONTENT).build())
                                       .build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSweepingOutputObtainValue() {
    SweepingOutputInstance savedSweepingOutputInstance =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutputInstance.getAppId())
                                       .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
                                       .workflowExecutionId(sweepingOutputInstance.getWorkflowExecutionIds().get(0))
                                       .build());

    assertThat(((SweepingOutputData) savedSweepingOutputInstance.getValue()).getText())
        .isEqualTo(SWEEPING_OUTPUT_CONTENT);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCopyOutputsForAnotherWorkflowExecution() {
    final String anotherWorkflowId = generateUuid();
    sweepingOutputService.copyOutputsForAnotherWorkflowExecution(
        sweepingOutputInstance.getAppId(), sweepingOutputInstance.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    SweepingOutputInstance savedSweepingOutputInstance1 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutputInstance.getAppId())
                                       .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
                                       .workflowExecutionId(sweepingOutputInstance.getWorkflowExecutionIds().get(0))
                                       .build());
    assertThat(savedSweepingOutputInstance1).isNotNull();
    assertThat(savedSweepingOutputInstance1.getWorkflowExecutionIds())
        .containsExactly(sweepingOutputInstance.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    SweepingOutputInstance savedSweepingOutputInstance2 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutputInstance.getAppId())
                                       .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
                                       .workflowExecutionId(anotherWorkflowId)
                                       .build());
    assertThat(savedSweepingOutputInstance2).isNotNull();
    assertThat(savedSweepingOutputInstance2.getWorkflowExecutionIds())
        .containsExactly(sweepingOutputInstance.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    assertThat(savedSweepingOutputInstance1.getUuid()).isEqualTo(sweepingOutputInstance.getUuid());
    assertThat(savedSweepingOutputInstance2.getUuid()).isEqualTo(sweepingOutputInstance.getUuid());

    assertThat(savedSweepingOutputInstance1.getOutput()).isEqualTo(sweepingOutputInstance.getOutput());
    assertThat(savedSweepingOutputInstance2.getOutput()).isEqualTo(sweepingOutputInstance.getOutput());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCopyOutputsForAnotherWorkflowExecutionForSameExecution() {
    sweepingOutputService.copyOutputsForAnotherWorkflowExecution(sweepingOutputInstance.getAppId(),
        sweepingOutputInstance.getWorkflowExecutionIds().get(0),
        sweepingOutputInstance.getWorkflowExecutionIds().get(0));

    SweepingOutputInstance savedSweepingOutputInstance1 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutputInstance.getAppId())
                                       .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
                                       .workflowExecutionId(sweepingOutputInstance.getWorkflowExecutionIds().get(0))
                                       .build());
    assertThat(savedSweepingOutputInstance1).isNotNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFindSweepingOutput() {
    SweepingOutput sweepingOutput = sweepingOutputService.findSweepingOutput(
        SweepingOutputInquiry.builder()
            .name(SWEEPING_OUTPUT_NAME)
            .appId(sweepingOutputInstance.getAppId())
            .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
            .workflowExecutionId(sweepingOutputInstance.getWorkflowExecutionIds().get(0))
            .build());
    assertThat(sweepingOutput).isNotNull();
    assertThat(sweepingOutput).isInstanceOf(SweepingOutputData.class);
    assertThat(((SweepingOutputData) sweepingOutput).getText()).isEqualTo(SWEEPING_OUTPUT_CONTENT);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFindSweepingOutputForNull() {
    SweepingOutput sweepingOutput = sweepingOutputService.findSweepingOutput(
        SweepingOutputInquiry.builder()
            .name("Some Name")
            .appId(sweepingOutputInstance.getAppId())
            .phaseExecutionId(sweepingOutputInstance.getPipelineExecutionId())
            .workflowExecutionId(sweepingOutputInstance.getWorkflowExecutionIds().get(0))
            .build());
    assertThat(sweepingOutput).isNull();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCleanForStateExecutionInstance() {
    sweepingOutputService.cleanForStateExecutionInstance(stateExecutionInstance);
    SweepingOutput sweepingOutput =
        sweepingOutputService.findSweepingOutput(SweepingOutputInquiry.builder()
                                                     .name(SWEEPING_OUTPUT_NAME)
                                                     .appId(appId)
                                                     .phaseExecutionId(sweepingOutputInstance.getPhaseExecutionId())
                                                     .workflowExecutionId(workflowExecutionUuid)
                                                     .stateExecutionId(stateExecutionInstanceId)
                                                     .build());
    assertThat(sweepingOutput).isNull();
  }
}