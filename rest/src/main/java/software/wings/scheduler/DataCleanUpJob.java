package software.wings.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.LogService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Cron that runs every mid night to cleanup the data
 * Created by sgurubelli on 7/19/17.
 */
public class DataCleanUpJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(DataCleanUpJob.class);
  private static final long ARTIFACT_RETENTION_SIZE = 25L;
  private static final long AUDIT_RETENTION_TIME = 7 * 24 * 60 * 60 * 1000L;
  private static final long ALERT_RETENTION_TIME = 7 * 24 * 60 * 60 * 1000L;
  public static final long LOGS_RETENTION_TIME = TimeUnit.DAYS.toMillis(1);

  @Inject private ArtifactService artifactService;
  @Inject private AuditService auditService;
  @Inject private AlertService alertService;
  @Inject private ExecutorService executorService;
  @Inject private LogService logService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Running Data Cleanup Job asynchronously and returning");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    logger.info("Running Data Cleanup Job");
    deleteArtifacts();
    deleteAuditRecords();
    deleteAlerts();
    logService.purgeActivityLogs();
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
  private void deleteAlerts() {
    try {
      logger.info("Deleting alerts");
      alertService.deleteOldAlerts(ALERT_RETENTION_TIME);
      logger.info("Deleting alerts success");
    } catch (Exception e) {
      logger.warn("Deleting alerts failed.", e);
    }
  }
}
