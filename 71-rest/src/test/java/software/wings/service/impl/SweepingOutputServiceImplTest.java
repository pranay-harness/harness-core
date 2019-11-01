package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutput.Scope;
import io.harness.beans.SweepingOutput.SweepingOutputBuilder;
import io.harness.category.element.UnitTests;
import io.harness.serializer.KryoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.SweepingOutputService.SweepingOutputInquiry;

public class SweepingOutputServiceImplTest extends WingsBaseTest {
  private static final String SWEEPING_OUTPUT_NAME = "SWEEPING_OUTPUT_NAME";
  private static final String SWEEPING_OUTPUT_CONTENT = "SWEEPING_OUTPUT_CONTENT";

  @InjectMocks @Inject private SweepingOutputService sweepingOutputService;

  private SweepingOutputBuilder sweepingOutputBuilder;
  private SweepingOutput sweepingOutput;

  @Before
  public void setup() {
    sweepingOutputBuilder = SweepingOutputServiceImpl.prepareSweepingOutputBuilder(
        generateUuid(), generateUuid(), generateUuid(), generateUuid(), generateUuid(), Scope.WORKFLOW);

    sweepingOutput = sweepingOutputService.save(
        sweepingOutputBuilder.name(SWEEPING_OUTPUT_NAME).output(KryoUtils.asBytes(SWEEPING_OUTPUT_CONTENT)).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testCopyOutputsForAnotherWorkflowExecution() {
    final String anotherWorkflowId = generateUuid();
    sweepingOutputService.copyOutputsForAnotherWorkflowExecution(
        sweepingOutput.getAppId(), sweepingOutput.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    SweepingOutput savedSweepingOutput1 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutput.getAppId())
                                       .phaseExecutionId(sweepingOutput.getPipelineExecutionId())
                                       .workflowExecutionId(sweepingOutput.getWorkflowExecutionIds().get(0))
                                       .build());
    assertThat(savedSweepingOutput1).isNotNull();
    assertThat(savedSweepingOutput1.getWorkflowExecutionIds())
        .containsExactly(sweepingOutput.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    SweepingOutput savedSweepingOutput2 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutput.getAppId())
                                       .phaseExecutionId(sweepingOutput.getPipelineExecutionId())
                                       .workflowExecutionId(anotherWorkflowId)
                                       .build());
    assertThat(savedSweepingOutput2).isNotNull();
    assertThat(savedSweepingOutput2.getWorkflowExecutionIds())
        .containsExactly(sweepingOutput.getWorkflowExecutionIds().get(0), anotherWorkflowId);

    assertThat(savedSweepingOutput1.getUuid()).isEqualTo(sweepingOutput.getUuid());
    assertThat(savedSweepingOutput2.getUuid()).isEqualTo(sweepingOutput.getUuid());

    assertThat(savedSweepingOutput1.getOutput()).isEqualTo(sweepingOutput.getOutput());
    assertThat(savedSweepingOutput2.getOutput()).isEqualTo(sweepingOutput.getOutput());
  }

  @Test
  @Category(UnitTests.class)
  public void testCopyOutputsForAnotherWorkflowExecutionForSameExecution() {
    sweepingOutputService.copyOutputsForAnotherWorkflowExecution(sweepingOutput.getAppId(),
        sweepingOutput.getWorkflowExecutionIds().get(0), sweepingOutput.getWorkflowExecutionIds().get(0));

    SweepingOutput savedSweepingOutput1 =
        sweepingOutputService.find(SweepingOutputInquiry.builder()
                                       .name(SWEEPING_OUTPUT_NAME)
                                       .appId(sweepingOutput.getAppId())
                                       .phaseExecutionId(sweepingOutput.getPipelineExecutionId())
                                       .workflowExecutionId(sweepingOutput.getWorkflowExecutionIds().get(0))
                                       .build());
    assertThat(savedSweepingOutput1).isNotNull();
  }
}