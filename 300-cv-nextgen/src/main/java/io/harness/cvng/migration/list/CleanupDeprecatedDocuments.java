package io.harness.cvng.migration.list;

import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class CleanupDeprecatedDocuments implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private CVConfigService cvConfigService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Override
  public void migrate() {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class).asList();
    for (ActivitySource activitySource : activitySources) {
      log.info("Deleting activity source: {}", activitySource);
      activitySourceService.deleteActivitySource(activitySource.getAccountId(), activitySource.getOrgIdentifier(),
          activitySource.getProjectIdentifier(), activitySource.getIdentifier());
      log.info("Deleted activity source: {}", activitySource);
    }

    List<VerificationJob> verificationJobs = hPersistence.createQuery(VerificationJob.class).asList();
    for (VerificationJob verificationJob : verificationJobs) {
      log.info("Deleting VerificationJob source: {}", verificationJob);
      verificationJobService.delete(verificationJob.getAccountId(), verificationJob.getOrgIdentifier(),
          verificationJob.getProjectIdentifier(), verificationJob.getIdentifier());
      log.info("Deleted Verification job source: {}", verificationJob);
    }

    List<CVConfig> cvConfigs = hPersistence.createQuery(CVConfig.class).asList();
    for (CVConfig cvConfig : cvConfigs) {
      log.info("Deleting CVConfig source: {}", cvConfig);
      if (isOldCVConfig(cvConfig)) {
        cvConfigService.delete(cvConfig.getUuid());
        monitoringSourcePerpetualTaskService.deleteTask(cvConfig.getAccountId(), cvConfig.getOrgIdentifier(),
            cvConfig.getProjectIdentifier(), cvConfig.getIdentifier());
        log.info("Deleted CVConfig job source: {}", cvConfig);
      } else {
        log.info("CVConfig belong to monitored service", cvConfig);
      }
    }
  }

  private boolean isOldCVConfig(CVConfig cvConfig) {
    return !cvConfig.getIdentifier().contains("/");
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
