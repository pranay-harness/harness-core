package software.wings.integration.migration.legacy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;
import software.wings.scheduler.DataCleanUpJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.LogService;

import java.util.concurrent.TimeUnit;

/**
 * Created by sgurubelli on 7/19/17.
 */
@Integration
@Ignore
public class DataCleanUpJobUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(DataCleanUpJobUtil.class);

  private static final int ARTIFACT_RETENTION_SIZE = 100;
  private static final long AUDIT_RETENTION_TIME = TimeUnit.DAYS.toMillis(7);
  private static final long ALERT_RETENTION_TIME = TimeUnit.DAYS.toMillis(7);

  @Inject private ArtifactService artifactService;
  @Inject private AuditService auditService;
  @Inject private AlertService alertService;
  @Inject private LogService logService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Test
  public void deleteArtifacts() {
    logger.info("Deleting artifacts");
    try {
      artifactService.deleteArtifacts(ARTIFACT_RETENTION_SIZE);
    } catch (Exception e) {
      logger.info("Deleting artifacts failed");
      logger.error("", e);
    }
    logger.info("Deleting artifacts completed");
  }

  @Test
  public void deleteArtifactFiles() {
    logger.info("Deleting artifact files");
    try {
      artifactService.deleteArtifactFiles();
    } catch (Exception e) {
      logger.info("Deleting artifacts failed");
      logger.error("", e);
    }
    logger.info("Deleting artifacts completed");
  }

  @Test
  public void deleteAuditRecords() {
    logger.info("Deleting audit records");
    try {
      auditService.deleteAuditRecords(AUDIT_RETENTION_TIME);
    } catch (Exception e) {
      logger.info("Deleting audit records failed");
      logger.error("", e);
    }
    logger.info("Deleting audit records completed");
  }

  @Test
  public void deleteAlerts() {
    logger.info("Deleting alerts");
    try {
      alertService.deleteOldAlerts(ALERT_RETENTION_TIME);
      logger.info("Deleting alerts success");
    } catch (Exception e) {
      logger.info("Deleting alerts failed.");
      logger.error("", e);
    }
    logger.info("Deleting alerts completed");
  }

  @Test
  public void deleteActivityLogs() {
    logger.info("Deleting alerts");
    try {
      logService.purgeActivityLogs();
      logger.info("Deleting alerts success");
    } catch (Exception e) {
      logger.info("Deleting alerts failed.");
      logger.error("", e);
    }
    logger.info("Deleting alerts completed");
  }

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void addCronForSystemDataCleanup() {
    logger.info("Adding System Data Cleanup cron");
    JobDetail job = JobBuilder.newJob(DataCleanUpJob.class)
                        .withIdentity("DATA_CLEANUP_CRON_NAME", "DATA_CLEANUP_CRON_GROUP")
                        .withDescription("Data cleanup job that deletes records")
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity("DATA_CLEANUP_CRON_NAME", "DATA_CLEANUP_CRON_NAME")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(30).repeatForever())
            .build();
    jobScheduler.scheduleJob(job, trigger);
    logger.info("Added System Data Cleanup cron");
  }

  @Test
  public void deleteCronForSystemDataCleanup() {
    jobScheduler.deleteJob("DATA_CLEANUP_CRON_NAME", "DATA_CLEANUP_CRON_NAME");
  }
}
