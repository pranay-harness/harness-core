package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.intfc.LearningEngineService;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Delete all learning engine tasks in queue older than 7 days.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LearningEngineTaskQueueCleanUpJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LearningEngineTaskQueueCleanUpJob.class);
  private static final int MAX_QUEUE_DELAY_MINS = 10;
  private static final int CLEANUP_PERIOD_HOURS = 3;
  private static final int MAX_QUEUE_RETENTION_DAYS = 7;

  @Inject private LearningEngineService learningEngineService;

  @Override
  public void execute(JobExecutionContext context) {
    long startMillis = context.getMergedJobDataMap().getLong("timestamp");
    long nowMillis = System.currentTimeMillis();

    if (shouldCleanupTasks(nowMillis, startMillis)) {
      cleanupTasks(keepAfterTimeMillis(nowMillis));
      context.getJobDetail().getJobDataMap().put("timestamp", nowMillis);
    }

    Optional<LearningEngineAnalysisTask> optionalAnalysisTask = learningEngineService.earliestQueued();
    if (optionalAnalysisTask.isPresent()) {
      if (isQueueStuck(nowMillis, optionalAnalysisTask.get().getCreatedAt())) {
        handleQueueStuck();
      }
    }
  }

  protected boolean shouldCleanupTasks(long nowMillis, long startMillis) {
    return nowMillis - startMillis > TimeUnit.HOURS.toMillis(CLEANUP_PERIOD_HOURS);
  }

  protected long keepAfterTimeMillis(long nowMillis) {
    return nowMillis - TimeUnit.DAYS.toMillis(MAX_QUEUE_RETENTION_DAYS);
  }

  protected void cleanupTasks(long keepAfterTimeMillis) {
    logger.info("Deleting learning engine tasks in queue that are older than {}", new Date(keepAfterTimeMillis));
    learningEngineService.cleanup(keepAfterTimeMillis);
  }

  protected boolean isQueueStuck(long nowMillis, long earliesQueueTimeMillis) {
    return nowMillis - earliesQueueTimeMillis > TimeUnit.MINUTES.toMillis(MAX_QUEUE_DELAY_MINS);
  }

  protected void handleQueueStuck() {
    logger.error("[learning-engine], task sitting in queue for more than {} mins. Check learning engine cluster health",
        MAX_QUEUE_DELAY_MINS);
  }
}
