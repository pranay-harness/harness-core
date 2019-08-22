package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.FeatureName;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.CVActivityLog.CVActivityLogKeys;
import software.wings.verification.CVActivityLog.LogLevel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CVActivityLogServiceTest extends BaseIntegrationTest {
  @Inject CVActivityLogService cvActivityLogService;
  @Mock FeatureFlagService featureFlagService;
  @Before
  public void setupTests() throws IllegalAccessException {
    FieldUtils.writeField(cvActivityLogService, "featureFlagService", featureFlagService, true);
    when(featureFlagService.isGlobalEnabled(FeatureName.CV_ACTIVITY_LOG)).thenReturn(true);
  }
  @Test
  @Category(IntegrationTests.class)
  public void testSavingLogIfFeatureFlagEnabled() {
    String cvConfigId = generateUUID();
    long now = System.currentTimeMillis();

    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertEquals("activity log from test", cvActivityLog.getLog());
    assertEquals(TimeUnit.MILLISECONDS.toMinutes(now), cvActivityLog.getDataCollectionMinute());
    assertEquals(Collections.emptyList(), cvActivityLog.getTimestampParams());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testSavingLogWithTimestampParams() {
    String cvConfigId = generateUUID();
    long now = System.currentTimeMillis();
    long nextMinute = Instant.ofEpochMilli(now).plus(1, ChronoUnit.MINUTES).toEpochMilli();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test at minute %t and another minute %t", now, nextMinute);
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog.getTimestampParams()).hasSize(2);
    assertEquals(nextMinute, (long) cvActivityLog.getTimestampParams().get(1));
    assertEquals(now, (long) cvActivityLog.getTimestampParams().get(0));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testGetAnsi() {
    String cvConfigId = generateUUID();
    long now = System.currentTimeMillis();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .error("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertEquals("\u001B[31mactivity log from test\u001B[0m", cvActivityLog.getAnsiLog());

    cvConfigId = generateUUID();
    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertEquals("activity log from test", cvActivityLog.getAnsiLog());
  }
  @Test
  @Category(IntegrationTests.class)
  public void testSavingLogIfFeatureFlagDisabled() {
    String cvConfigId = generateUUID();
    when(featureFlagService.isGlobalEnabled(FeatureName.CV_ACTIVITY_LOG)).thenReturn(false);
    long now = System.currentTimeMillis();

    cvActivityLogService.getLoggerByCVConfigId(cvConfigId, TimeUnit.MILLISECONDS.toMinutes(now))
        .info("activity log from test");
    CVActivityLog cvActivityLog =
        wingsPersistence.createQuery(CVActivityLog.class).filter(CVActivityLogKeys.cvConfigId, cvConfigId).get();
    assertThat(cvActivityLog).isNull();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testFindByCVConfigIdToReturnEmptyIfNoLogs() {
    String cvConfigId = generateUUID();
    assertThat(cvActivityLogService.findByCVConfigId(cvConfigId, 0, System.currentTimeMillis())).isEmpty();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testFindByCVConfigIdWithSameStartTimeAndEndTime() {
    String cvConfigId = generateUUID();
    long nowMilli = System.currentTimeMillis();
    String logLine = "test log message: " + generateUUID();
    long nowMinute = TimeUnit.MILLISECONDS.toMinutes(nowMilli);
    createLog(cvConfigId, nowMinute, logLine);
    createLog(cvConfigId, nowMinute + 1, "log line");
    assertEquals(Collections.emptyList(),
        cvActivityLogService.findByCVConfigId(
            cvConfigId, TimeUnit.MINUTES.toMillis(nowMinute), TimeUnit.MINUTES.toMillis(nowMinute - 1)));
    List<CVActivityLog> activityLogs = cvActivityLogService.findByCVConfigId(cvConfigId, nowMilli, nowMilli);
    assertThat(activityLogs).hasSize(1);
    assertEquals(cvConfigId, activityLogs.get(0).getCvConfigId());
    assertThat(activityLogs.get(0).getStateExecutionId()).isNull();
    assertEquals(nowMinute, activityLogs.get(0).getDataCollectionMinute());
    assertEquals(logLine, activityLogs.get(0).getLog());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testFindByStateExecutionId() {
    String stateExecutionId = generateUUID();
    String logLine = "test log message: " + generateUUID();
    cvActivityLogService.getLoggerByStateExecutionId(stateExecutionId).info(logLine);
    List<CVActivityLog> activityLogs = cvActivityLogService.findByStateExecutionId(stateExecutionId);
    assertThat(activityLogs).hasSize(1);
    assertEquals(stateExecutionId, activityLogs.get(0).getStateExecutionId());
    assertThat(activityLogs.get(0).getCvConfigId()).isNull();
    assertEquals(0, activityLogs.get(0).getDataCollectionMinute());
    assertEquals(logLine, activityLogs.get(0).getLog());
    assertEquals(LogLevel.INFO, activityLogs.get(0).getLogLevel());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testFindByCVConfigIdWithDiffSameStartTimeAndEndTime() {
    String cvConfigId = generateUUID();
    long now = System.currentTimeMillis();
    String logLine1 = "test log message: " + generateUUID();
    String logLine2 = "test log message: " + generateUUID();

    long nowMinute = TimeUnit.MILLISECONDS.toMinutes(now);
    createLog(cvConfigId, nowMinute, logLine1);
    createLog(cvConfigId, nowMinute + 1, logLine2);

    List<CVActivityLog> activityLogs = cvActivityLogService.findByCVConfigId(
        cvConfigId, TimeUnit.MINUTES.toMillis(nowMinute), TimeUnit.MINUTES.toMillis(nowMinute + 1));
    assertThat(activityLogs).hasSize(2);
    assertEquals(cvConfigId, activityLogs.get(0).getCvConfigId());
    assertEquals(nowMinute, activityLogs.get(0).getDataCollectionMinute());
    assertEquals(logLine1, activityLogs.get(0).getLog());
    assertEquals(cvConfigId, activityLogs.get(1).getCvConfigId());
    assertEquals(nowMinute + 1, activityLogs.get(1).getDataCollectionMinute());
    assertEquals(logLine2, activityLogs.get(1).getLog());
  }

  private void createLog(String cvConfigId, long dataCollectionMinute, String logLine) {
    CVActivityLog cvActivityLog = CVActivityLog.builder()
                                      .cvConfigId(cvConfigId)
                                      .dataCollectionMinute(dataCollectionMinute)
                                      .log(logLine)
                                      .logLevel(LogLevel.INFO)
                                      .build();
    wingsPersistence.save(cvActivityLog);
  }
}
