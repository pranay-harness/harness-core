package software.wings.integration.migration.legacy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.scheduler.DataCleanUpJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.LogService;

import java.util.ArrayList;
import java.util.List;
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
  @Inject private WingsPersistence wingsPersistence;

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

  @Ignore
  @Test
  public void purgeArtifacts() {
    /**
     * Get All applications
     *
     */
    List<String> appIds = wingsPersistence.getCollection("applications").distinct("appId");
    List<String> artifactAppIds = wingsPersistence.getCollection("artifacts").distinct("appId");
    List<String> orphanAppIds = new ArrayList<>();
    artifactAppIds.forEach(s -> {
      if (!appIds.contains(s)) {
        orphanAppIds.add(s);
        logger.info("AppId " + s + " does not exist anymore");
      } else {
        //        logger.info("AppId " + s + " exists");
      }
    });

    /**
     * Get all serviceIds
     *
     */
    List<String> serviceIds = wingsPersistence.getCollection("services").distinct("_id");
    List<String> artifactServiceIds = wingsPersistence.getCollection("artifacts").distinct("serviceIds");
    List<String> orphanServiceIds = new ArrayList<>();
    for (String artifactServiceId : artifactServiceIds) {
      if (!serviceIds.contains(artifactServiceId)) {
        orphanServiceIds.add(artifactServiceId);
        logger.info("ServiceId " + artifactServiceId + " does not exist anymore");
      }
    }
    logger.info("Orphan Service Ids " + orphanServiceIds.size());
    orphanServiceIds.forEach(s -> {
      Query artifactQuery = wingsPersistence.createQuery(Artifact.class)
                                .project("artifactFiles.fileUuid", true)
                                .field("serviceIds")
                                .equal(s)
                                .disableValidation();
      final MorphiaIterator<Artifact, Artifact> iterator = artifactQuery.fetch();
      List<Artifact> artifacts = new ArrayList<>();
      while (iterator.hasNext()) {
        artifacts.add(iterator.next());
      }
      deleteArtifacts(artifacts);
      logger.info("artifacts = " + artifacts);
    });

    /**
     * Get all serviceIds
     *
     */
    List<String> steamIds = wingsPersistence.getCollection("artifactStream").distinct("_id");
    List<String> artifactStreamIds = wingsPersistence.getCollection("artifacts").distinct("artifactStreamId");
    List<String> orphanStreamIds = new ArrayList<>();
    artifactStreamIds.forEach(s -> {
      if (!steamIds.contains(s)) {
        orphanStreamIds.add(s);
        logger.info("ArtifactStreamId " + s + " does not exist anymore");
      } else {
        //        logger.info("ServiceId " + s + " exists");
      }
    });
    logger.info("Orphan ArtifactSteam Ids " + orphanStreamIds.size());
    orphanStreamIds.forEach(s -> {
      Query artifactQuery = wingsPersistence.createQuery(Artifact.class)
                                .project("artifactFiles.fileUuid", true)
                                .field("artifactStreamId")
                                .equal(s)
                                .disableValidation();
      final MorphiaIterator<Artifact, Artifact> iterator = artifactQuery.fetch();
      List<Artifact> artifacts = new ArrayList<>();
      while (iterator.hasNext()) {
        artifacts.add(iterator.next());
      }
      logger.info("artifacts to be deleted = " + artifacts);
      deleteArtifacts(artifacts);
    });

    List<String> artifactStreamServiceIds = wingsPersistence.getCollection("artifactStream").distinct("serviceId");
    serviceIds = wingsPersistence.getCollection("services").distinct("_id");
    List<String> orphanStreamServiceIds = new ArrayList<>();
    for (String artifactStreamServiceId : artifactStreamServiceIds) {
      if (!serviceIds.contains(artifactStreamServiceId)) {
        orphanStreamServiceIds.add(artifactStreamServiceId);
        logger.info("ArtifactStream ServiceId " + artifactStreamServiceId + " does not exist anymore");
        wingsPersistence.getCollection("artifactStream")
            .remove(new BasicDBObject("serviceId", new BasicDBObject("$eq", artifactStreamServiceId)));
      } else {
        //        logger.info("ServiceId " + s + " exists");
      }
    }
    logger.info("Orphan artifact stream ids size = " + orphanStreamServiceIds.size());

    /**
     * Get the fileUuids from artifacts.. and delete one by one from the artifact files and chunks
     */
    List<String> artifactFileUuids = wingsPersistence.getCollection("artifacts").distinct("artifactFiles.fileUuid");
    List<ObjectId> artifactFileIds = wingsPersistence.getCollection("artifacts.files").distinct("_id");
    List<ObjectId> orphanArtifactFileIds = new ArrayList<>();

    artifactFileIds.forEach(s -> {
      if (!artifactFileUuids.contains(s.toHexString())) {
        logger.info("s = " + s);
        orphanArtifactFileIds.add(s);
      }
    });
    logger.info("orphanArtifactFileIds size = " + orphanArtifactFileIds.size());
    orphanArtifactFileIds.forEach(objectId -> {
      logger.info("Deleting artifactFileUuids of artifacts.files {}", objectId);
      wingsPersistence.getCollection("artifacts.files")
          .remove(new BasicDBObject("_id", new BasicDBObject("$eq", objectId)));
      wingsPersistence.getCollection("artifacts.chunks")
          .remove(new BasicDBObject("files_id", new BasicDBObject("$eq", objectId)));
      logger.info("Deleting artifactFileUuids of artifacts.files {} success", objectId);

    });
  }

  private void deleteArtifacts(List<Artifact> toBeDeletedArtifacts) {
    try {
      List<ObjectId> artifactFileUuids = new ArrayList<>();
      for (Artifact artifact : toBeDeletedArtifacts) {
        for (ArtifactFile artifactFile : artifact.getArtifactFiles()) {
          if (artifactFile.getFileUuid() != null) {
            artifactFileUuids.add(new ObjectId(artifactFile.getFileUuid()));
          }
        }
      }
      if (!artifactFileUuids.isEmpty()) {
        Object[] artifactIds = toBeDeletedArtifacts.stream().map(Artifact::getUuid).toArray();
        logger.info("Deleting artifactIds of artifacts {}", artifactIds);
        wingsPersistence.getCollection("artifacts")
            .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds)));
        logger.info("Deleting artifactFileUuids of artifacts.files {}", artifactFileUuids.toArray());
        wingsPersistence.getCollection("artifacts.files")
            .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
        logger.info("Deleting files_id of artifacts {}", artifactFileUuids.toArray());
        wingsPersistence.getCollection("artifacts.chunks")
            .remove(new BasicDBObject("files_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
      }
    } catch (Exception ex) {
      logger.warn("Failed to delete artifacts");
    }
  }
}
