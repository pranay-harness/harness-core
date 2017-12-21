package software.wings.scheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AppContainer.Builder.anAppContainer;

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
import software.wings.beans.AppContainer;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

public class PruneFileJobTest extends WingsBaseTest {
  public static final Logger logger = LoggerFactory.getLogger(PruneFileJobTest.class);

  @Mock private WingsPersistence wingsPersistence;

  @Mock private FileService fileService;

  @Mock private QuartzScheduler jobScheduler;

  @Inject @InjectMocks PruneFileJob job;

  private final static String OBJECT_ID = "object_id";

  private final static String FILE1_ID = "file1";
  private final static String FILE2_ID = "file2";
  private final static String FILE3_ID = "file3";

  public JobDetail details(String className, String objectId, FileBucket bucket, List<String> uuids) {
    return JobBuilder.newJob(PruneFileJob.class)
        .withIdentity(OBJECT_ID, PruneFileJob.GROUP)
        .usingJobData(PruneFileJob.OBJECT_CLASS_KEY, className)
        .usingJobData(PruneFileJob.OBJECT_ID_KEY, objectId)
        .usingJobData(PruneFileJob.BUCKET_KEY, bucket.name())
        .usingJobData(PruneFileJob.UUIDS_KEY, String.join(",", uuids))
        .build();
  }

  public JobDetail details(Class cls, String objectId, FileBucket bucket, List<String> uuids) {
    return details(cls.getCanonicalName(), objectId, bucket, uuids);
  }

  @Test
  public void selfPruneTheJobWhenSucceed() throws Exception {
    when(wingsPersistence.get(Artifact.class, OBJECT_ID)).thenReturn(null);
    doNothing().when(fileService).deleteFile(OBJECT_ID, FileBucket.PLATFORMS);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(Artifact.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));

    job.execute(context);

    verify(wingsPersistence, times(1)).get(Artifact.class, OBJECT_ID);
    verify(fileService, times(1)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThereFirstTime() throws Exception {
    when(wingsPersistence.get(AppContainer.class, OBJECT_ID)).thenReturn(anAppContainer().build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(AppContainer.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));
    when(context.getPreviousFireTime()).thenReturn(null);

    job.execute(context);

    verify(fileService, times(0)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(0)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void selfPruneTheJobIfServiceStillThere() throws Exception {
    when(wingsPersistence.get(AppContainer.class, OBJECT_ID)).thenReturn(anAppContainer().build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(AppContainer.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));
    when(context.getPreviousFireTime()).thenReturn(new Date());

    job.execute(context);

    verify(fileService, times(0)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void UnhandledClass() throws Exception {
    when(wingsPersistence.get(Base.class, OBJECT_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(Base.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));

    Logger mockLogger = mock(Logger.class);
    Whitebox.setInternalState(job, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1)).error(any(String.class), matches(Base.class.getCanonicalName()));
    verify(fileService, times(0)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void WrongClass() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(Base.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));

    job.execute(context);

    verify(fileService, times(0)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void retryIfServiceThrew() throws Exception {
    when(wingsPersistence.get(Environment.class, OBJECT_ID)).thenReturn(null);

    doThrow(new WingsException("Forced exception")).when(fileService).deleteFile(FILE1_ID, FileBucket.PLATFORMS);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(details(AppContainer.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID)));

    job.execute(context);

    verify(jobScheduler, times(0)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }

  @Test
  public void multipleUuids() throws Exception {
    when(wingsPersistence.get(Artifact.class, OBJECT_ID)).thenReturn(null);
    doNothing().when(fileService).deleteFile(OBJECT_ID, FileBucket.PLATFORMS);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail())
        .thenReturn(
            details(Artifact.class, OBJECT_ID, FileBucket.PLATFORMS, Arrays.asList(FILE1_ID, FILE2_ID, FILE3_ID)));

    job.execute(context);

    verify(wingsPersistence, times(1)).get(Artifact.class, OBJECT_ID);
    verify(fileService, times(1)).deleteFile(FILE1_ID, FileBucket.PLATFORMS);
    verify(fileService, times(1)).deleteFile(FILE2_ID, FileBucket.PLATFORMS);
    verify(fileService, times(1)).deleteFile(FILE3_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(OBJECT_ID, PruneFileJob.GROUP);
  }
}
