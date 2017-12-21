package software.wings.scheduler;

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

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;

import java.util.Date;
import javax.inject.Inject;

public class PruneObjectJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(PruneObjectJobTest.class);

  @Mock private WingsPersistence wingsPersistence;

  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;

  @Mock private QuartzScheduler jobScheduler;

  @Inject @InjectMocks PruneObjectJob job;

  private final static String APP_ID = "app_id";
  private final static String OBJECT_ID = "object_id";

  public JobDetail details(String className, String appId, String objectId) {
    return JobBuilder.newJob(PruneObjectJob.class)
        .withIdentity(OBJECT_ID, PruneObjectJob.GROUP)
        .usingJobData(PruneObjectJob.OBJECT_CLASS_KEY, className)
        .usingJobData(PruneObjectJob.APP_ID_KEY, appId)
        .usingJobData(PruneObjectJob.OBJECT_ID_KEY, objectId)
        .build();
  }

  public JobDetail details(Class cls, String appId, String objectId) {
    return details(cls.getCanonicalName(), appId, objectId);
  }

  @Test
  public void selfPruneTheJobWhenSucceed() throws Exception {
    when(wingsPersistence.get(Environment.class, OBJECT_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, OBJECT_ID));

    job.execute(context);

    verify(environmentService, times(1)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThereFirstTime() throws Exception {
    when(wingsPersistence.get(Application.class, OBJECT_ID))
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class, APP_ID, OBJECT_ID));
    when(context.getPreviousFireTime()).thenReturn(null);

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(0)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThere() throws Exception {
    when(wingsPersistence.get(Environment.class, OBJECT_ID))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(OBJECT_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, OBJECT_ID));
    when(context.getPreviousFireTime()).thenReturn(new Date());

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void UnhandledClass() throws Exception {
    when(wingsPersistence.get(Base.class, OBJECT_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Base.class, APP_ID, OBJECT_ID));

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(job, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1)).error(any(String.class), matches(Base.class.getCanonicalName()));
    verify(environmentService, times(0)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void WrongClass() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details("foo", APP_ID, OBJECT_ID));

    job.execute(context);

    verify(environmentService, times(0)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void retryIfServiceThrew() throws Exception {
    when(wingsPersistence.get(Environment.class, OBJECT_ID)).thenReturn(null);

    doThrow(new WingsException("Forced exception")).when(environmentService).pruneDescendingObjects(APP_ID, OBJECT_ID);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Environment.class, APP_ID, OBJECT_ID));

    job.execute(context);

    verify(jobScheduler, times(0)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }

  @Test
  public void differentAppIdAndObjectIdForApplication() throws Exception {
    when(wingsPersistence.get(Application.class, OBJECT_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class, APP_ID, OBJECT_ID));

    assertThatThrownBy(() -> job.execute(context)).isInstanceOf(WingsException.class);

    verify(environmentService, times(0)).pruneDescendingObjects(APP_ID, OBJECT_ID);
    verify(jobScheduler, times(0)).deleteJob(OBJECT_ID, PruneObjectJob.GROUP);
  }
}
