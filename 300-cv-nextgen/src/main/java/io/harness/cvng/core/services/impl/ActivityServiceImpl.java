package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.ActivityDTO;
import io.harness.cvng.core.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityKeys;
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
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ActivityServiceImpl implements ActivityService {
  private static final int RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE = 5;
  @Inject private WebhookService webhookService;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private NextGenService nextGenService;

  @Override
  public void register(String accountId, String webhookToken, ActivityDTO activityDTO) {
    webhookService.validateWebhookToken(
        webhookToken, activityDTO.getProjectIdentifier(), activityDTO.getOrgIdentifier());
    Preconditions.checkNotNull(activityDTO);
    Activity activity = activityDTO.toEntity();
    activity.validate();
    if (activity.getType().equals(ActivityType.DEPLOYMENT)) {
      // TODO: Remove this "if" when we have support for all types of verification trigger from activity.
      activity.setVerificationJobInstanceIds(createVerificationJobInstances(activity));
    }
    logger.info("Registering a new activity of type {} for account {}", activity.getType(), accountId);
    hPersistence.save(activity);
  }

  @Override
  public List<DeploymentActivityVerificationResultDTO> getRecentDeploymentActivityVerifications(
      String accountId, String projectIdentifier) {
    List<DeploymentGroupByTag> recentDeploymentActivities = getRecentDeploymentActivities(accountId, projectIdentifier);
    List<DeploymentActivityVerificationResultDTO> results = new ArrayList<>();
    recentDeploymentActivities.forEach(deploymentGroupByTag -> {
      List<String> verificationJobInstanceIds =
          getVerificationJobInstanceIds(deploymentGroupByTag.getDeploymentActivities());
      DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
          verificationJobInstanceService.getAggregatedVerificationResult(verificationJobInstanceIds);
      deploymentActivityVerificationResultDTO.setTag(deploymentGroupByTag.getDeploymentTag());
      Activity firstActivity = deploymentGroupByTag.deploymentActivities.get(0);
      // TODO: do we need to implement caching?
      String serviceName = nextGenService
                               .getService(firstActivity.getServiceIdentifier(), firstActivity.getAccountIdentifier(),
                                   firstActivity.getOrgIdentifier(), firstActivity.getProjectIdentifier())
                               .getName();
      deploymentActivityVerificationResultDTO.setServiceName(serviceName);
      results.add(deploymentActivityVerificationResultDTO);
    });

    return results;
  }

  private List<String> getVerificationJobInstanceIds(List<DeploymentActivity> deploymentActivities) {
    return deploymentActivities.stream()
        .map(deploymentActivity -> deploymentActivity.getVerificationJobInstanceIds())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
  private List<DeploymentGroupByTag> getRecentDeploymentActivities(String accountId, String projectIdentifier) {
    List<DeploymentActivity> activities =
        (List<DeploymentActivity>) (List<?>) hPersistence.createQuery(Activity.class)
            .filter(ActivityKeys.accountIdentifier, accountId)
            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
            .filter(ActivityKeys.type, ActivityType.DEPLOYMENT)
            .order(Sort.descending(ActivityKeys.createdAt))
            // assumption is that the latest 5 tags will be part of last 1000 deployments
            .asList(new FindOptions().limit(1000));
    Map<String, DeploymentGroupByTag> groupByTagMap = new HashMap<>();
    List<DeploymentGroupByTag> result = new ArrayList<>();
    for (DeploymentActivity activity : activities) {
      DeploymentGroupByTag deploymentGroupByTag;
      if (groupByTagMap.containsKey(activity.getDeploymentTag())) {
        deploymentGroupByTag = groupByTagMap.get(activity.getDeploymentTag());
      } else {
        if (groupByTagMap.size() < RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE) {
          deploymentGroupByTag = DeploymentGroupByTag.builder().deploymentTag(activity.getDeploymentTag()).build();
          result.add(deploymentGroupByTag);
        } else {
          // ignore the tag that is not in the latest 5 tags.
          continue;
        }
      }
      deploymentGroupByTag.addDeploymentActivity(activity);
    }
    return result;
  }
  @Data
  @Builder
  private static class DeploymentGroupByTag {
    String deploymentTag;
    List<DeploymentActivity> deploymentActivities;

    public void addDeploymentActivity(DeploymentActivity deploymentActivity) {
      if (deploymentActivities == null) {
        deploymentActivities = new ArrayList<>();
      }
      deploymentActivities.add(deploymentActivity);
    }
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
          verificationJobInstance.setStartTime(deploymentActivity.getVerificationStartTime());
          verificationJobInstance.setDataCollectionDelay(deploymentActivity.getDataCollectionDelay());
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
        .resolvedJob(verificationJob)
        .build();
  }
}
