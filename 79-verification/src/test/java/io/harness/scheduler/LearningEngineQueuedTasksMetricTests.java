package io.harness.scheduler;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_WORKFLOW_TASK_COUNT;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS;

import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;

public class LearningEngineQueuedTasksMetricTests extends VerificationBaseTest {
  @Inject private ServiceGuardAccountPoller serviceGuardAccountPoller;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private HarnessMetricRegistry harnessMetricRegistry;

  @Before
  public void setup() throws IllegalAccessException {
    doNothing().when(harnessMetricRegistry).recordGaugeValue(any(), any(String[].class), anyDouble());
    FieldUtils.writeField(serviceGuardAccountPoller, "metricRegistry", harnessMetricRegistry, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNoQueuedTasks() {
    serviceGuardAccountPoller.recordQueuedTaskMetric();
    ArgumentCaptor<String> taskCaptorName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> taskCaptorParams = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Double> taskCaptorValue = ArgumentCaptor.forClass(Double.class);

    verify(harnessMetricRegistry, times(15))
        .recordGaugeValue(taskCaptorName.capture(), taskCaptorParams.capture(), taskCaptorValue.capture());

    verifyMetricsPublished(taskCaptorName);

    taskCaptorValue.getAllValues().forEach(value -> assertThat(value).isEqualTo(0.0));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testQueuedTasks() {
    wingsPersistence.save(LearningEngineAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                              .is24x7Task(true)
                              .build());
    wingsPersistence.save(LearningEngineAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                              .is24x7Task(true)
                              .build());
    wingsPersistence.save(LearningEngineAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.FEEDBACK_ANALYSIS)
                              .is24x7Task(true)
                              .build());
    wingsPersistence.save(LearningEngineAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                              .is24x7Task(false)
                              .build());
    wingsPersistence.save(LearningEngineExperimentalAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                              .is24x7Task(false)
                              .build());
    wingsPersistence.save(LearningEngineAnalysisTask.builder()
                              .cvConfigId(generateUuid())
                              .executionStatus(ExecutionStatus.QUEUED)
                              .ml_analysis_type(MLAnalysisType.LOG_CLUSTER)
                              .is24x7Task(false)
                              .build());

    sleep(ofSeconds(1));

    serviceGuardAccountPoller.recordQueuedTaskMetric();
    ArgumentCaptor<String> taskCaptorName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> taskCaptorParams = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Double> taskCaptorValue = ArgumentCaptor.forClass(Double.class);

    verify(harnessMetricRegistry, times(15))
        .recordGaugeValue(taskCaptorName.capture(), taskCaptorParams.capture(), taskCaptorValue.capture());

    verifyMetricsPublished(taskCaptorName);

    taskCaptorValue.getAllValues().forEach(value -> assertThat(value).isGreaterThan(0.0));
  }

  private void verifyMetricsPublished(ArgumentCaptor<String> taskCaptorName) {
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_CLUSTERING_TASK_QUEUED_COUNT);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_ANALYSIS_TASK_QUEUED_COUNT);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_FEEDBACK_TASK_QUEUED_COUNT);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_EXP_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues())
        .contains(LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_SERVICE_GUARD_ANALYSIS_TASK_QUEUED_COUNT);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_COUNT);
    assertThat(taskCaptorName.getAllValues())
        .contains(LEARNING_ENGINE_SERVICE_GUARD_CLUSTERING_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_WORKFLOW_TASK_QUEUED_TIME_IN_SECONDS);
    assertThat(taskCaptorName.getAllValues()).contains(LEARNING_ENGINE_WORKFLOW_TASK_COUNT);
  }
}
