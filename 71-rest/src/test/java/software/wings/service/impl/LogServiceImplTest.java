package software.wings.service.impl;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Log;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DataStoreService;

public class LogServiceImplTest extends WingsBaseTest {
  @Mock private DataStoreService dataStoreService;
  @Mock private ActivityService activityService;
  @InjectMocks @Inject private LogServiceImpl logService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldCallSaveIfCompeted() {
    final Log logObject = sampleLog(SUCCESS);
    logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, "Execute", logObject);
    verify(dataStoreService, times(1)).save(eq(Log.class), eq(Lists.newArrayList(logObject)), eq(false));
    verify(activityService, times(1)).updateCommandUnitStatus(APP_ID, ACTIVITY_ID, "Execute", SUCCESS);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldCallSaveIfRunning() {
    final Log logObject = sampleLog(RUNNING);
    logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, "Execute", logObject);
    verify(dataStoreService, times(1)).save(eq(Log.class), eq(Lists.newArrayList(logObject)), eq(false));
    verify(activityService, times(1)).updateCommandUnitStatus(APP_ID, ACTIVITY_ID, "Execute", RUNNING);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldSkipSaveIfExceedCountAndRunning() {
    final Log logObject = sampleLog(RUNNING);
    doReturn(999999999).when(dataStoreService).getNumberOfResults(eq(Log.class), any(PageRequest.class));
    logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, "Execute", logObject);
    verify(dataStoreService, never()).save(eq(Log.class), eq(Lists.newArrayList(logObject)), eq(false));
    verify(activityService, never()).updateCommandUnitStatus(APP_ID, ACTIVITY_ID, "Execute", SUCCESS);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldSaveIfExceedCountAndFailure() {
    final Log logObject = sampleLog(FAILURE);
    doReturn(999999999).when(dataStoreService).getNumberOfResults(eq(Log.class), any(PageRequest.class));
    logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, "Execute", logObject);
    verify(dataStoreService, times(1)).save(eq(Log.class), eq(Lists.newArrayList(logObject)), eq(false));
    verify(activityService, times(1)).updateCommandUnitStatus(APP_ID, ACTIVITY_ID, "Execute", FAILURE);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldSaveIfExceedCountAndSuccess() {
    final Log logObject = sampleLog(SUCCESS);
    doReturn(999999999).when(dataStoreService).getNumberOfResults(eq(Log.class), any(PageRequest.class));
    logService.batchedSaveCommandUnitLogs(ACTIVITY_ID, "Execute", logObject);
    verify(dataStoreService, times(1)).save(eq(Log.class), eq(Lists.newArrayList(logObject)), eq(false));
    verify(activityService, times(1)).updateCommandUnitStatus(APP_ID, ACTIVITY_ID, "Execute", SUCCESS);
  }

  private Log sampleLog(CommandExecutionStatus status) {
    return Log.Builder.aLog()
        .activityId(ACCOUNT_ID)
        .activityId(ACTIVITY_ID)
        .appId(APP_ID)
        .commandUnitName("Execute")
        .logLevel(LogLevel.INFO)
        .executionResult(status)
        .logLine("Fetching data from....")
        .build();
  }
}
