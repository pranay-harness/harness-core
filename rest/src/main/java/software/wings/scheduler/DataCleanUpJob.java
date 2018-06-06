package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.LogService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cron that runs every mid night to cleanup the data
 * Created by sgurubelli on 7/19/17.
 */
public class DataCleanUpJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(DataCleanUpJob.class);
  private static final int ARTIFACT_RETENTION_SIZE = 25;
  private static final long AUDIT_RETENTION_TIME = TimeUnit.DAYS.toMillis(7);
  private static final long ALERT_RETENTION_TIME = TimeUnit.DAYS.toMillis(7);
  private static final long DELEGATE_TASK_RETENTION_TIME = TimeUnit.HOURS.toMillis(48);
  public static final long LOGS_RETENTION_TIME = TimeUnit.DAYS.toMillis(30);

  @Inject private ArtifactService artifactService;
  @Inject private AuditService auditService;
  @Inject private AlertService alertService;
  @Inject private DelegateService delegateService;
  @Inject private ExecutorService executorService;
  @Inject private LogService logService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    logger.info("Running Data Cleanup Job");
    deleteArtifacts();
    // Not purging audit and activity logs any more
    // deleteAuditRecords();
    // deleteActivityLogs();
    deleteDelegateTasks();
    logger.info("Running Data Cleanup Job complete");
  }

  private void deleteArtifacts() {
    try {
      logger.info("Deleting artifacts");
      artifactService.deleteArtifacts(ARTIFACT_RETENTION_SIZE);
      logger.info("Deleting artifacts success");
    } catch (Exception e) {
      logger.warn("Deleting artifacts failed.", e);
    }
  }

  private void deleteAuditRecords() {
    try {
      logger.info("Deleting audit records");
      auditService.deleteAuditRecords(AUDIT_RETENTION_TIME);
      logger.info("Deleting audit records success");
    } catch (Exception e) {
      logger.warn("Deleting audit records failed.", e);
    }
  }

  private void deleteDelegateTasks() {
    try {
      logger.info("Deleting delegate tasks");
      delegateService.deleteOldTasks(DELEGATE_TASK_RETENTION_TIME);
      logger.info("Deleting delegate tasks success");
    } catch (Exception e) {
      logger.warn("Deleting delegate tasks failed.", e);
    }
  }

  private void deleteActivityLogs() {
    try {
      logger.info("Deleting activity logs");
      logService.purgeActivityLogs();
      logger.info("Deleting activity logs success");
    } catch (Exception e) {
      logger.warn("Deleting activity logs failed.", e);
    }
  }
}
