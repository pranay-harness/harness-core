package software.wings.service.impl.analysis;

import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceSecretKey;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesDataRecord;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.VerificationService;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, VerificationServiceImpl.class})
@PowerMockIgnore({"javax.net.*", "javax.crypto.*"})
public class LearningEngineAnalysisServiceImplTest extends WingsBaseTest {
  @Inject private VerificationService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;

  private String cvConfigId;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    cvConfigId = generateUUID();
    PowerMockito.mockStatic(System.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestTaskForCvConfigIds_EmptyConfigIds() {
    Optional<LearningEngineAnalysisTask> task = learningEngineService.getLatestTaskForCvConfigIds(null);
    assertThat(task).isEmpty();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestTaskForCvConfigIds_EmptyTask() {
    Optional<LearningEngineAnalysisTask> task =
        learningEngineService.getLatestTaskForCvConfigIds(Collections.singletonList(cvConfigId));
    assertThat(task).isEmpty();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetLatestTaskForCvConfigIds_NonEmptyTask() {
    LearningEngineAnalysisTask logMLTask = LearningEngineAnalysisTask.builder()
                                               .cvConfigId(cvConfigId)
                                               .analysis_minute(1)
                                               .executionStatus(ExecutionStatus.SUCCESS)
                                               .ml_analysis_type(MLAnalysisType.LOG_ML)
                                               .build();
    wingsPersistence.save(logMLTask);
    LearningEngineAnalysisTask timeSeriesTask = LearningEngineAnalysisTask.builder()
                                                    .cvConfigId(cvConfigId)
                                                    .analysis_minute(2)
                                                    .executionStatus(ExecutionStatus.FAILED)
                                                    .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                                                    .build();
    wingsPersistence.save(timeSeriesTask);
    LearningEngineAnalysisTask timeSeriesQueuedTask = LearningEngineAnalysisTask.builder()
                                                          .cvConfigId(cvConfigId)
                                                          .analysis_minute(3)
                                                          .executionStatus(ExecutionStatus.QUEUED)
                                                          .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                                                          .build();
    wingsPersistence.save(timeSeriesQueuedTask);
    LearningEngineAnalysisTask feedbackTask = LearningEngineAnalysisTask.builder()
                                                  .cvConfigId(cvConfigId)
                                                  .analysis_minute(4)
                                                  .executionStatus(ExecutionStatus.FAILED)
                                                  .ml_analysis_type(MLAnalysisType.FEEDBACK_ANALYSIS)
                                                  .build();
    wingsPersistence.save(feedbackTask);
    LearningEngineAnalysisTask timeSeriesNewConfigTask = LearningEngineAnalysisTask.builder()
                                                             .cvConfigId("config2")
                                                             .analysis_minute(5)
                                                             .executionStatus(ExecutionStatus.FAILED)
                                                             .ml_analysis_type(MLAnalysisType.TIME_SERIES)
                                                             .build();
    wingsPersistence.save(timeSeriesNewConfigTask);

    Optional<LearningEngineAnalysisTask> learningEngineAnalysisTask =
        learningEngineService.getLatestTaskForCvConfigIds(Collections.singletonList(cvConfigId));
    assertThat(learningEngineAnalysisTask).isPresent();
    learningEngineAnalysisTask.ifPresent(task -> {
      assertThat(task.getAnalysis_minute()).isEqualTo(2);
      assertThat(task.getMl_analysis_type()).isEqualTo(MLAnalysisType.TIME_SERIES);
    });
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Log_NoData() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_ML, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Log_WithDataOutsideWindow() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    LogDataRecord logDataRecord = LogDataRecord.builder().cvConfigId(cvConfigId).timeStamp(minute - 20).build();
    wingsPersistence.save(logDataRecord);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_ML, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Log_WithOtherConfigId() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    LogDataRecord logDataRecord = LogDataRecord.builder().cvConfigId("config2").timeStamp(minute - 10).build();
    wingsPersistence.save(logDataRecord);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_ML, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Log_WithHearBeatRecord() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    LogDataRecord logDataRecord =
        LogDataRecord.builder().cvConfigId(cvConfigId).clusterLevel(ClusterLevel.H0).timeStamp(minute - 10).build();
    wingsPersistence.save(logDataRecord);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_ML, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Log_WithData() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    LogDataRecord logDataRecord =
        LogDataRecord.builder().cvConfigId(cvConfigId).clusterLevel(ClusterLevel.L2).timeStamp(minute - 10).build();
    wingsPersistence.save(logDataRecord);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_ML, minute);
    assertThat(hasData).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_LogCluster_WithData() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    LogDataRecord logDataRecord =
        LogDataRecord.builder().cvConfigId(cvConfigId).clusterLevel(ClusterLevel.L1).timeStamp(minute - 10).build();
    wingsPersistence.save(logDataRecord);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.LOG_CLUSTER, minute);
    assertThat(hasData).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Metric_NoData() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.TIME_SERIES, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Metric_WithDataOutsideWindow() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    TimeSeriesDataRecord record = TimeSeriesDataRecord.builder().cvConfigId(cvConfigId).timeStamp(minute - 20).build();
    wingsPersistence.save(record);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.TIME_SERIES, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Metric_WithOtherConfigId() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    TimeSeriesDataRecord record = TimeSeriesDataRecord.builder().cvConfigId("config2").timeStamp(minute - 10).build();
    wingsPersistence.save(record);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.TIME_SERIES, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Metric_WithHearBeatRecord() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    TimeSeriesDataRecord record =
        TimeSeriesDataRecord.builder().cvConfigId(cvConfigId).level(ClusterLevel.H0).timeStamp(minute - 10).build();
    wingsPersistence.save(record);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.TIME_SERIES, minute);
    assertThat(hasData).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Metric_WithData() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    TimeSeriesDataRecord record = TimeSeriesDataRecord.builder().cvConfigId(cvConfigId).timeStamp(minute - 10).build();
    wingsPersistence.save(record);
    boolean hasData = learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.TIME_SERIES, minute);
    assertThat(hasData).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCheckIfAnalysisHasData_Feedback() {
    long minute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    assertThatThrownBy(
        () -> learningEngineService.checkIfAnalysisHasData(cvConfigId, MLAnalysisType.FEEDBACK_ANALYSIS, minute))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void test_getVerificationServiceSecretKey_whenEnvVariableDefined() {
    String verificationServiceSecret = generateUUID();
    PowerMockito.when(System.getenv(VerificationConstants.VERIFICATION_SERVICE_SECRET))
        .thenReturn(verificationServiceSecret);
    assertThat(learningEngineService.getVerificationServiceSecretKey()).isEqualTo(verificationServiceSecret);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void test_getVerificationServiceSecretKey_whenEnvVariableNotDefined() {
    ServiceSecretKey serviceSecretKey = wingsPersistence.createQuery(ServiceSecretKey.class).get();
    assertThat(serviceSecretKey).isNull();
    learningEngineService.initializeServiceSecretKeys();
    serviceSecretKey = wingsPersistence.createQuery(ServiceSecretKey.class).get();
    assertThat(learningEngineService.getVerificationServiceSecretKey()).isEqualTo(serviceSecretKey.getServiceSecret());
  }
}