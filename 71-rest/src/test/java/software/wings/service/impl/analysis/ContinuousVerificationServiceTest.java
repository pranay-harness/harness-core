package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.ExecutionDataValue;
import software.wings.resources.ContinuousVerificationResource;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
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
import java.util.concurrent.TimeUnit;

public class ContinuousVerificationServiceTest extends WingsBaseTest {
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private ContinuousVerificationResource continuousVerificationResource;

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
            .canaryNewHostNames(Sets.newHashSet("host1", "host2", "controlNode-1", "controlNode-2", "testNode-1"))
            .lastExecutionNodes(Sets.newHashSet("host3", "host4", "controlNode-3", "controlNode-4", "testNode-2"))
            .query(generateUuid())
            .baselineExecutionId(generateUuid())
            .correlationId(generateUuid())
            .analysisMinute(5)
            .build();
    stateExecutionMap.put(displayName, verificationStateAnalysisExecutionData);

    wingsPersistence.updateField(StateExecutionInstance.class, stateExecutionId,
        StateExecutionInstanceKeys.stateExecutionMap, stateExecutionMap);
    wingsPersistence.updateField(
        StateExecutionInstance.class, stateExecutionId, StateExecutionInstanceKeys.displayName, displayName);
    final AnalysisContext analysisContext =
        AnalysisContext.builder().timeDuration(8).stateExecutionId(stateExecutionId).build();
    wingsPersistence.save(analysisContext);

    VerificationStateAnalysisExecutionData verificationStateExecutionData =
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
    assertThat(verificationStateExecutionData.getAnalysisMinute())
        .isEqualTo(verificationStateAnalysisExecutionData.getAnalysisMinute());
    assertThat(verificationStateExecutionData.getProgressPercentage()).isEqualTo(0);
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES + 1);

    // test one record
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord1 = new NewRelicMetricAnalysisRecord();
    newRelicMetricAnalysisRecord1.setStateExecutionId(stateExecutionId);
    newRelicMetricAnalysisRecord1.setAnalysisMinute(1);
    newRelicMetricAnalysisRecord1.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(8));
    wingsPersistence.save(newRelicMetricAnalysisRecord1);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES);

    // test two records
    NewRelicMetricAnalysisRecord newRelicMetricAnalysisRecord2 = new NewRelicMetricAnalysisRecord();
    newRelicMetricAnalysisRecord2.setStateExecutionId(stateExecutionId);
    newRelicMetricAnalysisRecord2.setAnalysisMinute(2);
    newRelicMetricAnalysisRecord2.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2));
    wingsPersistence.save(newRelicMetricAnalysisRecord2);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 * 2 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes((analysisContext.getTimeDuration() - 2)
            * ((newRelicMetricAnalysisRecord2.getCreatedAt() - newRelicMetricAnalysisRecord1.getCreatedAt()) / 2)));

    wingsPersistence.delete(wingsPersistence.createQuery(NewRelicMetricAnalysisRecord.class, excludeAuthority));

    // test one record
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord1 = new TimeSeriesMLAnalysisRecord();
    timeSeriesMLAnalysisRecord1.setStateExecutionId(stateExecutionId);
    timeSeriesMLAnalysisRecord1.setAnalysisMinute(1);
    timeSeriesMLAnalysisRecord1.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(12));
    wingsPersistence.save(timeSeriesMLAnalysisRecord1);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(analysisContext.getTimeDuration() + DELAY_MINUTES);

    // test two records
    TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord2 = new TimeSeriesMLAnalysisRecord();
    timeSeriesMLAnalysisRecord2.setStateExecutionId(stateExecutionId);
    timeSeriesMLAnalysisRecord2.setCreatedAt(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2));
    timeSeriesMLAnalysisRecord2.setAnalysisMinute(2);
    wingsPersistence.save(timeSeriesMLAnalysisRecord2);

    verificationStateExecutionData = continuousVerificationService.getVerificationStateExecutionData(stateExecutionId);
    assertThat(verificationStateExecutionData.getProgressPercentage())
        .isEqualTo(100 * 2 / analysisContext.getTimeDuration());
    assertThat(verificationStateExecutionData.getRemainingMinutes())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes((analysisContext.getTimeDuration() - 2)
            * ((timeSeriesMLAnalysisRecord2.getCreatedAt() - timeSeriesMLAnalysisRecord1.getCreatedAt()) / 2)));

    // test the nodes
    final Map<String, ExecutionDataValue> executionSummary =
        verificationStateAnalysisExecutionData.getExecutionSummary();
    assertThat(executionSummary.size()).isGreaterThan(0);
    assertThat(executionSummary.get("newVersionNodes").getValue()).isEqualTo(Sets.newHashSet("host1", "host2"));
    assertThat(executionSummary.get("previousVersionNodes").getValue()).isEqualTo(Sets.newHashSet("host3", "host4"));
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
  public void testGetCVCertifiedDetailsForWorkflowFromResource() {
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

    RestResponse<List<CVCertifiedDetailsForWorkflowState>> result =
        continuousVerificationResource.getCVCertifiedLabelsForWorkflow(accountId, appId, workflowExecutionId);
    List<CVCertifiedDetailsForWorkflowState> stateExecutionInstances = result.getResource();
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
  public void testGetCVCertifiedDetailsForPipelineFromResource() {
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

    RestResponse<List<CVCertifiedDetailsForWorkflowState>> stateExecutionInstances =
        continuousVerificationResource.getCVCertifiedLabelsForPipeline(accountId, appId, pipelineExecutionId);
    List<CVCertifiedDetailsForWorkflowState> result = stateExecutionInstances.getResource();
    List<String> states = Arrays.asList("SPLUNKV2", "APP_DYNAMICS");
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0).getExecutionDetails().getStateType()).isIn(states);
    assertThat(result.get(1).getExecutionDetails().getStateType()).isIn(states);
    assertThat(result.get(0).getPhaseName()).isEqualTo("Phase 1");
    assertThat(result.get(1).getPhaseName()).isEqualTo("Phase 1");
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
