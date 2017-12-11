package software.wings.scheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import org.junit.Ignore;
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
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Integration;
import software.wings.service.intfc.AppService;

import java.util.Date;
import javax.inject.Inject;

@Integration
public class PruneObjectJobMockTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(PruneObjectJob.class);

  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private QuartzScheduler jobScheduler;

  @Inject @InjectMocks PruneObjectJob job;

  private final static String objectId = "object_id";

  public JobDetail details(String className) {
    return JobBuilder.newJob(PruneObjectJob.class)
        .withIdentity(objectId, PruneObjectJob.GROUP)
        .usingJobData(PruneObjectJob.OBJECT_CLASS_KEY, className)
        .usingJobData(PruneObjectJob.OBJECT_ID_KEY, objectId)
        .build();
  }

  public JobDetail details(Class cls) {
    return details(cls.getCanonicalName());
  }

  @Test
  public void selfPruneTheJobWhenSucceed() throws Exception {
    when(wingsPersistence.get(Application.class, objectId)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class));

    job.execute(context);

    verify(appService, times(1)).pruneDescendingObjects(objectId);
    verify(jobScheduler, times(1)).deleteJob(objectId, PruneObjectJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThereFirstTime() throws Exception {
    when(wingsPersistence.get(Application.class, objectId))
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class));
    when(context.getPreviousFireTime()).thenReturn(null);

    job.execute(context);

    verify(appService, times(0)).pruneDescendingObjects(objectId);
    verify(jobScheduler, times(0)).deleteJob(objectId, PruneObjectJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThere() throws Exception {
    when(wingsPersistence.get(Application.class, objectId))
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class));
    when(context.getPreviousFireTime()).thenReturn(new Date());

    job.execute(context);

    verify(appService, times(0)).pruneDescendingObjects(objectId);
    verify(jobScheduler, times(1)).deleteJob(objectId, PruneObjectJob.GROUP);
  }

  @Test
  public void UnhandledClass() throws Exception {
    when(wingsPersistence.get(Base.class, objectId)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Base.class));

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(job, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1)).error(any(String.class), matches(Base.class.getCanonicalName()));
    verify(appService, times(0)).pruneDescendingObjects(objectId);
    verify(jobScheduler, times(1)).deleteJob(objectId, PruneObjectJob.GROUP);
  }

  @Test
  public void WrongClass() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details("foo"));

    job.execute(context);

    verify(appService, times(0)).pruneDescendingObjects(objectId);
    verify(jobScheduler, times(1)).deleteJob(objectId, PruneObjectJob.GROUP);
  }

  @Test
  @Ignore
  public void retryIfServiceThrew() throws Exception {
    when(wingsPersistence.get(Application.class, objectId)).thenReturn(null);

    doThrow(new WingsException()).when(appService).pruneDescendingObjects(objectId);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Application.class));

    job.execute(context);

    verify(jobScheduler, times(0)).deleteJob(objectId, PruneObjectJob.GROUP);
  }
}
