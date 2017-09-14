package software.wings.integration.migration;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.scheduler.DataCleanUpJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;

/**
 * Created by sgurubelli on 7/19/17.
 */
@Integration
@Ignore
public class DataCleanUpJobUtil extends WingsBaseTest {
  @Inject private ArtifactService artifactService;
  @Inject private AuditService auditService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  private static final long ARTIFACT_RETENTION_SIZE = 25L;
  private static final long AUDIT_RETENTION_TIME = 7 * 24 * 60 * 60 * 1000L;

  @Test
  public void deleteArtifacts() {
    System.out.println("Deleting artifacts");
    try {
      artifactService.deleteArtifacts(ARTIFACT_RETENTION_SIZE);
    } catch (Exception e) {
      System.out.println("Deleting artifacts failed");
      e.printStackTrace();
    }
    System.out.println("Deleting artifacts completed");
  }

  @Test
  public void deleteArtifactFiles() {
    System.out.println("Deleting artifact files");
    try {
      artifactService.deleteArtifactFiles();
    } catch (Exception e) {
      System.out.println("Deleting artifacts failed");
      e.printStackTrace();
    }
    System.out.println("Deleting artifacts completed");
  }

  @Test
  public void deleteAuditRecords() {
    System.out.println("Deleting audit records");
    try {
      auditService.deleteAuditRecords(AUDIT_RETENTION_TIME);
    } catch (Exception e) {
      System.out.println("Deleting audit records failed");
      e.printStackTrace();
    }
    System.out.println("Deleting audit records completed");
  }

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void addCronForSystemDataCleanup() {
    System.out.println("Adding System Data Cleanup cron");
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
    System.out.println("Added System Data Cleanup cron");
  }

  @Test
  public void deleteCronForSystemDataCleanup() {
    jobScheduler.deleteJob("DATA_CLEANUP_CRON_NAME", "DATA_CLEANUP_CRON_NAME");
  }
}
