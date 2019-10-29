package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.Builder;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateType;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContinuousVerificationServiceTest extends WingsBaseTest {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Test
  @Category(UnitTests.class)
  public void testGetVerificationStateExecutionData() {
    assertThat(continuousVerificationService.getVerificationStateExecutionData(generateUuid())).isNull();
    String stateExecutionId = wingsPersistence.save(Builder.aStateExecutionInstance().build());
    assertThat(continuousVerificationService.getVerificationStateExecutionData(stateExecutionId)).isNull();

    final String displayName = "new relic";
    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    final VerificationStateAnalysisExecutionData verificationStateAnalysisExecutionData =
        VerificationStateAnalysisExecutionData.builder()
            .stateExecutionInstanceId(stateExecutionId)
            .canaryNewHostNames(Sets.newHashSet("host1", "host2"))
            .lastExecutionNodes(Sets.newHashSet("host3", "host4"))
            .query(generateUuid())
            .baselineExecutionId(generateUuid())
            .mlAnalysisType(MLAnalysisType.TIME_SERIES)
            .timeDuration(10)
            .appId(generateUuid())
            .correlationId(generateUuid())
            .analysisMinute(5)
            .build();
    stateExecutionMap.put(displayName, verificationStateAnalysisExecutionData);

    wingsPersistence.updateField(StateExecutionInstance.class, stateExecutionId,
        StateExecutionInstanceKeys.stateExecutionMap, stateExecutionMap);
    wingsPersistence.updateField(
        StateExecutionInstance.class, stateExecutionId, StateExecutionInstanceKeys.displayName, displayName);
    final VerificationStateAnalysisExecutionData verificationStateExecutionData =
        continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData).isNotNull();
    assertThat(verificationStateExecutionData.getStateExecutionInstanceId())
        .isEqualTo(verificationStateAnalysisExecutionData.getStateExecutionInstanceId());
    assertThat(verificationStateExecutionData.getCanaryNewHostNames())
        .isEqualTo(verificationStateAnalysisExecutionData.getCanaryNewHostNames());
    assertThat(verificationStateExecutionData.getLastExecutionNodes())
        .isEqualTo(verificationStateAnalysisExecutionData.getLastExecutionNodes());
    assertThat(verificationStateExecutionData.getQuery()).isEqualTo(verificationStateAnalysisExecutionData.getQuery());
    assertThat(verificationStateExecutionData.getBaselineExecutionId())
        .isEqualTo(verificationStateAnalysisExecutionData.getBaselineExecutionId());
    assertThat(verificationStateExecutionData.getMlAnalysisType())
        .isEqualTo(verificationStateAnalysisExecutionData.getMlAnalysisType());
    assertThat(verificationStateExecutionData.getTimeDuration())
        .isEqualTo(verificationStateAnalysisExecutionData.getTimeDuration());
    assertThat(verificationStateExecutionData.getAnalysisMinute())
        .isEqualTo(verificationStateAnalysisExecutionData.getAnalysisMinute());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForWorkflow() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.ELK.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.NEW_RELIC.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.FAILED)
                                                         .build());

    String workflowExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .workflowExecutionId(workflowExecutionId)
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .workflowExecutionId(workflowExecutionId)
                              .build());

    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForWorkflow(accountId, appId, workflowExecutionId);
    assertThat(stateExecutionInstances).isNotEmpty();
    assertThat(stateExecutionInstances.size()).isEqualTo(2);
    List<String> states = Arrays.asList("ELK", "NEW_RELIC");
    assertThat(stateExecutionInstances.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(1).getExecutionDetails().getStateType()).isIn(states);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForPipeline() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String stateExecutionId1 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.SPLUNKV2.name())
                                                         .appId(appId)
                                                         .status(ExecutionStatus.SUCCESS)
                                                         .build());
    String stateExecutionId2 = wingsPersistence.save(Builder.aStateExecutionInstance()
                                                         .stateType(StateType.APP_DYNAMICS.name())
                                                         .status(ExecutionStatus.FAILED)
                                                         .appId(appId)
                                                         .build());

    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId1)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());
    wingsPersistence.save(ContinuousVerificationExecutionMetaData.builder()
                              .accountId(accountId)
                              .stateExecutionId(stateExecutionId2)
                              .pipelineExecutionId(pipelineExecutionId)
                              .workflowExecutionId(workflowExecutionId)
                              .phaseName("Phase 1")
                              .build());

    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForPipeline(accountId, appId, pipelineExecutionId);
    List<String> states = Arrays.asList("SPLUNKV2", "APP_DYNAMICS");
    assertThat(stateExecutionInstances).isNotEmpty();
    assertThat(stateExecutionInstances.size()).isEqualTo(2);
    assertThat(stateExecutionInstances.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(1).getExecutionDetails().getStateType()).isIn(states);
    assertThat(stateExecutionInstances.get(0).getPhaseName()).isEqualTo("Phase 1");
    assertThat(stateExecutionInstances.get(1).getPhaseName()).isEqualTo("Phase 1");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForPipelineNoCVStates() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForPipeline(accountId, appId, pipelineExecutionId);
    assertThat(stateExecutionInstances).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetCVCertifiedDetailsForWorkflowNoCVStates() {
    String accountId = generateUuid();
    String appId = generateUuid();
    String workflowExecutionId = generateUuid(), pipelineExecutionId = generateUuid();
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances =
        continuousVerificationService.getCVCertifiedDetailsForWorkflow(accountId, appId, workflowExecutionId);
    assertThat(stateExecutionInstances).isEmpty();
  }
}
