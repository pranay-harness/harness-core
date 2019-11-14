package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog;
import software.wings.verification.CVActivityLog.CVActivityLogKeys;
import software.wings.verification.CVActivityLog.LogLevel;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CVActivityLogServiceTest extends WingsBaseTest {
  @Inject CVActivityLogService cvActivityLogService;
  @Mock FeatureFlagService featureFlagService;
  private String stateExecutionId;
  @Before
  public void setupTests() throws IllegalAccessException {
    FieldUtils.writeField(cvActivityLogService, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isGlobalEnabled(FeatureName.CV_ACTIVITY_LOG)).thenReturn(true);
    stateExecutionId = generateUuid();
  }
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSavingLogIfFeatureFlagEnabled() {
    String cvConfigId = generateUuid();
    long now = System.currentTimeMillis();

    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog.getLog()).isEqualTo("activity log from test");
    assertThat(cvActivityLog.getDataCollectionMinute()).isEqualTo(TimeUnit.MILLISECONDS.toMinutes(now));
    assertThat(cvActivityLog.getTimestampParams()).isEqualTo(Collections.emptyList());
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSavingLogWithTimestampParams() {
    String cvConfigId = generateUuid();
    long now = System.currentTimeMillis();
    long nextMinute = Instant.ofEpochMilli(now).plus(1, ChronoUnit.MINUTES).toEpochMilli();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test at minute %t and another minute %t", now, nextMinute);
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog.getTimestampParams()).hasSize(2);
    assertThat((long) cvActivityLog.getTimestampParams().get(1)).isEqualTo(nextMinute);
    assertThat((long) cvActivityLog.getTimestampParams().get(0)).isEqualTo(now);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetAnsi() {
    String cvConfigId = generateUuid();
    long now = System.currentTimeMillis();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .error("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog.getAnsiLog()).isEqualTo("\u001B[31mactivity log from test\u001B[0m");

    cvConfigId = generateUuid();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog.getAnsiLog()).isEqualTo("activity log from test");
  }
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSavingLogIfFeatureFlagDisabled() {
    String cvConfigId = generateUuid();
    when(featureFlagService.isGlobalEnabled(FeatureName.CV_ACTIVITY_LOG)).thenReturn(false);
    long now = System.currentTimeMillis();

    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog).isNull();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFindByCVConfigIdToReturnEmptyIfNoLogs() {
    String cvConfigId = generateUuid();
    assertThat(cvActivityLogService.findByCVConfigId(cvConfigId, 0, System.currentTimeMillis())).isEmpty();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFindByCVConfigIdWithSameStartTimeAndEndTime() {
    String cvConfigId = generateUuid();
    long nowMilli = System.currentTimeMillis();
    String logLine = "test log message: " + generateUuid();
    long nowMinute = TimeUnit.MILLISECONDS.toMinutes(nowMilli);
    createLog(cvConfigId, nowMinute, logLine);
    createLog(cvConfigId, nowMinute + 1, "log line");
    assertThat(cvActivityLogService.findByCVConfigId(
                   cvConfigId, TimeUnit.MINUTES.toMillis(nowMinute), TimeUnit.MINUTES.toMillis(nowMinute - 1)))
        .isEqualTo(Collections.emptyList());
    List<CVActivityLog> activityLogs = cvActivityLogService.findByCVConfigId(cvConfigId, nowMilli, nowMilli);
    assertThat(activityLogs).hasSize(1);
    assertThat(activityLogs.get(0).getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(activityLogs.get(0).getStateExecutionId()).isNull();
    assertThat(activityLogs.get(0).getDataCollectionMinute()).isEqualTo(nowMinute);
    assertThat(activityLogs.get(0).getLog()).isEqualTo(logLine);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFindByStateExecutionId() {
    String stateExecutionId = generateUuid();
    String logLine = "test log message: " + generateUuid();
    cvActivityLogService.getLoggerByStateExecutionId(stateExecutionId).info(logLine);
    List<CVActivityLog> activityLogs = cvActivityLogService.findByStateExecutionId(stateExecutionId);
    assertThat(activityLogs).hasSize(1);
    assertThat(activityLogs.get(0).getStateExecutionId()).isEqualTo(stateExecutionId);
    assertThat(activityLogs.get(0).getCvConfigId()).isNull();
    assertThat(activityLogs.get(0).getDataCollectionMinute()).isEqualTo(0);
    assertThat(activityLogs.get(0).getLog()).isEqualTo(logLine);
    assertThat(activityLogs.get(0).getLogLevel()).isEqualTo(LogLevel.INFO);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFindByCVConfigIdWithDiffSameStartTimeAndEndTime() {
    String cvConfigId = generateUuid();
    long now = System.currentTimeMillis();
    String logLine1 = "test log message: " + generateUuid();
    String logLine2 = "test log message: " + generateUuid();

    long nowMinute = TimeUnit.MILLISECONDS.toMinutes(now);
    createLog(cvConfigId, nowMinute, logLine1);
    createLog(cvConfigId, nowMinute + 1, logLine2);

    List<CVActivityLog> activityLogs = cvActivityLogService.findByCVConfigId(
        cvConfigId, TimeUnit.MINUTES.toMillis(nowMinute), TimeUnit.MINUTES.toMillis(nowMinute + 1));
    assertThat(activityLogs).hasSize(2);
    assertThat(activityLogs.get(0).getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(activityLogs.get(0).getDataCollectionMinute()).isEqualTo(nowMinute);
    assertThat(activityLogs.get(0).getLog()).isEqualTo(logLine1);
    assertThat(activityLogs.get(1).getCvConfigId()).isEqualTo(cvConfigId);
    assertThat(activityLogs.get(1).getDataCollectionMinute()).isEqualTo(nowMinute + 1);
    assertThat(activityLogs.get(1).getLog()).isEqualTo(logLine2);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSaveActivityLogs() {
    List<CVActivityLog> cvActivityLogs =
        IntStream.range(0, 10).mapToObj(i -> getActivityLog()).collect(Collectors.toList());
    cvActivityLogService.saveActivityLogs(cvActivityLogs);
    List<CVActivityLog> savedActivityLogs = cvActivityLogService.findByStateExecutionId(stateExecutionId);
    assertThat(cvActivityLogs).isEqualTo(savedActivityLogs);
  }
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testIfCVTaskValidUntilIsBeingSetTo2Weeks() {
    CVActivityLog cvActivityLog = createLog(UUID.randomUUID().toString(), System.currentTimeMillis(), "Test log");
    assertThat(cvActivityLog.getValidUntil().getTime() > Instant.now().toEpochMilli()).isTrue();
    assertThat(Math.abs(cvActivityLog.getValidUntil().getTime()
                   - OffsetDateTime.now().plus(2, ChronoUnit.WEEKS).toInstant().toEpochMilli())
        < TimeUnit.DAYS.toMillis(1));
  }

  private CVActivityLog createLog(String cvConfigId, long dataCollectionMinute, String logLine) {
    CVActivityLog cvActivityLog = CVActivityLog.builder()
                                      .cvConfigId(cvConfigId)
                                      .dataCollectionMinute(dataCollectionMinute)
                                      .log(logLine)
                                      .logLevel(LogLevel.INFO)
                                      .build();
    wingsPersistence.save(cvActivityLog);
    return cvActivityLog;
  }

  private CVActivityLog getActivityLog() {
    return CVActivityLog.builder()
        .stateExecutionId(stateExecutionId)
        .logLevel(LogLevel.INFO)
        .log("test log: " + generateUuid())
        .build();
  }
}
