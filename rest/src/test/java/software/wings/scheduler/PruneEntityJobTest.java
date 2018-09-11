package software.wings.scheduler;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.MockableTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.LogService;

import java.util.Date;

public class PruneEntityJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(PruneEntityJobTest.class);

  @Mock private WingsPersistence wingsPersistence;

  @Mock private LogService logService;
  @Inject @InjectMocks private ActivityService activityService;

  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;

  @Mock private QuartzScheduler jobScheduler;

  @Inject @InjectMocks PruneEntityJob job;

  private static final String APP_ID = "app_id";
  private static final String ENTITY_ID = "entityId";

  public JobDetail details(String className, String appId, String entityId) {
    return JobBuilder.newJob(PruneEntityJob.class)
        .withIdentity(ENTITY_ID, PruneEntityJob.GROUP)
        .usingJobData(PruneEntityJob.ENTITY_CLASS_KEY, className)
        .usingJobData(PruneEntityJob.APP_ID_KEY, appId)
        .usingJobData(PruneEntityJob.ENTITY_ID_KEY, entityId)
        .build();
  }

  public JobDetail details(Class cls, String appId, String entityId) {
    return details(cls.getCanonicalName(), appId, entityId);
  }

  @Test
  public void selfPruneTheJobWhenSucceed() throws Exception {
    when(wingsPersistence.get(Environment.class, ENTITY_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, ENTITY_ID));

    job.execute(context);

    verify(environmentService, times(1)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThereFirstTime() throws Exception {
    when(wingsPersistence.get(Application.class, ENTITY_ID))
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class, APP_ID, ENTITY_ID));
    when(context.getPreviousFireTime()).thenReturn(null);

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThere() throws Exception {
    when(wingsPersistence.get(Environment.class, ENTITY_ID))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENTITY_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, ENTITY_ID));
    when(context.getPreviousFireTime()).thenReturn(new Date());

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void unhandledClass() throws Exception {
    when(wingsPersistence.get(Base.class, ENTITY_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Base.class, APP_ID, ENTITY_ID));

    Logger mockLogger = mock(Logger.class);
    MockableTest.setStaticFieldValue(PruneEntityJob.class, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1)).error(any(String.class), matches(Base.class.getCanonicalName()));
    verify(environmentService, times(0)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void wrongClass() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details("foo", APP_ID, ENTITY_ID));

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void retryIfServiceThrew() throws Exception {
    when(wingsPersistence.get(Environment.class, ENTITY_ID)).thenReturn(null);

    doThrow(new WingsException("Forced exception")).when(environmentService).pruneDescendingEntities(APP_ID, ENTITY_ID);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, ENTITY_ID));

    job.execute(context);

    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void verifyThrowFromDescendingEntity() throws Exception {
    when(wingsPersistence.get(Activity.class, ENTITY_ID)).thenReturn(null);

    WingsException exception = new WingsException(DEFAULT_ERROR_CODE);
    doThrow(exception).when(logService).pruneByActivity(APP_ID, ENTITY_ID);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Activity.class, APP_ID, ENTITY_ID));

    Logger mockLogger = mock(Logger.class);
    MockableTest.setStaticFieldValue(PruneEntityJob.class, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1))
        .error(matches("Response message: An error has occurred. Please contact the Harness support team.\n"
                   + "Exception occurred: DEFAULT_ERROR_CODE"),
            any(WingsException.class));

    verify(logService, times(1)).pruneByActivity(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }

  @Test
  public void differentAppIdAndObjectIdForApplication() throws Exception {
    when(wingsPersistence.get(Application.class, ENTITY_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class, APP_ID, ENTITY_ID));

    assertThatThrownBy(() -> job.execute(context)).isInstanceOf(WingsException.class);

    verify(environmentService, times(0)).pruneDescendingEntities(APP_ID, ENTITY_ID);
    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneEntityJob.GROUP);
  }
}
