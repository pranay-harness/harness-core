package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.entities.DeploymentActivity;
import io.harness.cvng.core.services.api.ActivityService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ActivityServiceImpl implements ActivityService {
  @Inject private WebhookService webhookService;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;

  @Override
  public void register(String accountId, String webhookToken, ActivityDTO activityDTO) {
    webhookService.validateWebhookToken(
        webhookToken, activityDTO.getProjectIdentifier(), activityDTO.getOrgIdentifier());
    Preconditions.checkNotNull(activityDTO);
    Activity activity = activityDTO.toEntity();
    activity.validate();
    if (activity.getType().equals(ActivityType.DEPLOYMENT)) {
      // TODO: Remove this "if" when we have support for all types of verification trigger from activity.
      activity.setVerificationJobInstances(createVerificationJobInstances(activity));
    }
    logger.info("Registering a new activity of type {} for account {}", activity.getType(), accountId);
    hPersistence.save(activity);
  }

  private List<String> createVerificationJobInstances(Activity activity) {
    List<VerificationJobInstance> jobInstancesToCreate = new ArrayList<>();
    activity.getVerificationJobRuntimeDetails().forEach(jobDetail -> {
      String jobIdentifier = jobDetail.getVerificationJobIdentifier();
      Preconditions.checkNotNull(jobIdentifier, "Job Identifier must be present in the jobs to trigger");
      VerificationJob verificationJob =
          verificationJobService.getVerificationJob(activity.getAccountIdentifier(), jobIdentifier);
      Preconditions.checkNotNull(verificationJob, "No Job exists for verificationJobIdentifier: '%s'", jobIdentifier);
      VerificationJobInstance verificationJobInstance = fillOutCommonJobInstanceProperties(
          activity, verificationJob.resolveVerificationJob(jobDetail.getRuntimeValues()));
      switch (activity.getType()) {
        case DEPLOYMENT:
          DeploymentActivity deploymentActivity = (DeploymentActivity) activity;
          verificationJobInstance.setOldVersionHosts(deploymentActivity.getOldVersionHosts());
          verificationJobInstance.setNewVersionHosts(deploymentActivity.getNewVersionHosts());
          verificationJobInstance.setNewHostsTrafficSplitPercentage(
              deploymentActivity.getNewHostsTrafficSplitPercentage());
          verificationJobInstance.setDataCollectionDelay(
              Duration.ofMillis(deploymentActivity.getDataCollectionDelayMs()));
          break;
        default:
          logger.info("We currently support verification triggers only for canary deployment activity");
          return;
      }
      jobInstancesToCreate.add(verificationJobInstance);
    });
    return verificationJobInstanceService.create(jobInstancesToCreate);
  }

  private VerificationJobInstance fillOutCommonJobInstanceProperties(
      Activity activity, VerificationJob verificationJob) {
    return VerificationJobInstance.builder()
        .verificationJobIdentifier(verificationJob.getIdentifier())
        .accountId(activity.getAccountIdentifier())
        .executionStatus(ExecutionStatus.QUEUED)
        .deploymentStartTime(activity.getActivityStartTime())
        // TODO: Figure this out
        .startTime(activity.getActivityEndTime())
        .resolvedJob(verificationJob)
        .build();
  }
}
