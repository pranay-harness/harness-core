package software.wings.scheduler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import software.wings.WingsBaseTest;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LearningEngineTaskQueueCleanUpJobTest extends WingsBaseTest {
  @Mock JobExecutionContext context;
  @Mock JobDetail jobDetail;
  @Mock LearningEngineService learningEngineService;
  @Mock LearningEngineAnalysisTask learningEngineAnalysisTask;

  LearningEngineTaskQueueCleanUpJob job = spy(new LearningEngineTaskQueueCleanUpJob());
  JobDataMap jobDataMap = new JobDataMap();

  @Before
  public void setup() {
    when(context.getJobDetail()).thenReturn(jobDetail);
    setInternalState(job, "learningEngineService", learningEngineService);
    jobDataMap.put("timestamp", System.currentTimeMillis());
    when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    when(learningEngineService.earliestQueued()).thenReturn(Optional.empty());
  }

  @Test
  public void execute() throws Exception {
    job.execute(context);
    verify(job, never()).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
  }

  @Test
  public void executeCleanUp() throws Exception {
    long startTimeMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4);
    jobDataMap.put("timestamp", startTimeMillis);
    job.execute(context);
    verify(job, times(1)).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
    job.execute(context);
    verify(job, times(1)).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
  }

  @Test
  public void executeRepeatedCleanUp() throws Exception {
    long startTimeMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4);
    jobDataMap.put("timestamp", startTimeMillis);
    job.execute(context);
    verify(job, times(1)).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
    startTimeMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4);
    jobDataMap.put("timestamp", startTimeMillis);
    job.execute(context);
    verify(job, times(2)).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
  }

  @Test
  public void executeNoCleanUp() throws Exception {
    jobDataMap.put("timestamp", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2));
    job.execute(context);
    verify(job, never()).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
  }

  @Test
  public void executeStuckUp() throws Exception {
    when(learningEngineService.earliestQueued()).thenReturn(Optional.of(learningEngineAnalysisTask));
    when(learningEngineAnalysisTask.getCreatedAt())
        .thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20));

    jobDataMap.put("timestamp", System.currentTimeMillis());
    job.execute(context);
    verify(job, never()).cleanupTasks(anyLong());
    verify(job, times(1)).handleQueueStuck();
  }

  @Test
  public void executeNoStuckUp() throws Exception {
    when(learningEngineService.earliestQueued()).thenReturn(Optional.of(learningEngineAnalysisTask));
    when(learningEngineAnalysisTask.getCreatedAt())
        .thenReturn(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));

    jobDataMap.put("timestamp", System.currentTimeMillis());
    job.execute(context);
    verify(job, never()).cleanupTasks(anyLong());
    verify(job, never()).handleQueueStuck();
  }

  @Test
  public void maxQueueRetentionDays() {
    long now = System.currentTimeMillis();
    assertEquals(job.keepAfterTimeMillis(now), now - TimeUnit.DAYS.toMillis(7));
  }
}
