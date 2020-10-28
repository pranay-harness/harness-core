package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.activity.beans.DeploymentActivityPopoverResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentResultSummary;
import io.harness.cvng.activity.beans.DeploymentActivityVerificationResultDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.CustomActivity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.entities.DeploymentActivity.DeploymentActivityKeys;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.persistence.HPersistence;
import io.harness.persistence.HQuery;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ActivityServiceImpl implements ActivityService {
  private static final int RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE = 5;
  @Inject private WebhookService webhookService;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private NextGenService nextGenService;

  @Override
  public void register(String accountId, String webhookToken, ActivityDTO activityDTO) {
    webhookService.validateWebhookToken(
        webhookToken, activityDTO.getProjectIdentifier(), activityDTO.getOrgIdentifier());
    Preconditions.checkNotNull(activityDTO);
    Activity activity = getActivityFromDTO(activityDTO);
    activity.validate();
    if (activity.getType().equals(ActivityType.DEPLOYMENT)) {
      // TODO: Remove this "if" when we have support for all types of verification trigger from activity.
      activity.setVerificationJobInstanceIds(createVerificationJobInstances(activity));
    }
    hPersistence.save(activity);
  }

  public String createActivity(Activity activity) {
    return hPersistence.save(activity);
  }

  @Override
  public List<DeploymentActivityVerificationResultDTO> getRecentDeploymentActivityVerifications(
      String accountId, String orgIdentifier, String projectIdentifier) {
    List<DeploymentGroupByTag> recentDeploymentActivities =
        getRecentDeploymentActivities(accountId, orgIdentifier, projectIdentifier);
    List<DeploymentActivityVerificationResultDTO> results = new ArrayList<>();
    recentDeploymentActivities.forEach(deploymentGroupByTag -> {
      List<String> verificationJobInstanceIds =
          getVerificationJobInstanceIds(deploymentGroupByTag.getDeploymentActivities());
      DeploymentActivityVerificationResultDTO deploymentActivityVerificationResultDTO =
          verificationJobInstanceService.getAggregatedVerificationResult(verificationJobInstanceIds);
      deploymentActivityVerificationResultDTO.setTag(deploymentGroupByTag.getDeploymentTag());
      Activity firstActivity = deploymentGroupByTag.deploymentActivities.get(0);
      // TODO: do we need to implement caching?
      String serviceName = getServiceNameFromActivity(firstActivity);
      deploymentActivityVerificationResultDTO.setServiceName(serviceName);
      deploymentActivityVerificationResultDTO.setServiceIdentifier(firstActivity.getServiceIdentifier());
      results.add(deploymentActivityVerificationResultDTO);
    });

    return results;
  }

  @Override
  public DeploymentActivityResultDTO getDeploymentActivityVerificationsByTag(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, String deploymentTag) {
    List<DeploymentActivity> deploymentActivities =
        getDeploymentActivitiesByTag(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag);
    DeploymentResultSummary deploymentResultSummary =
        DeploymentResultSummary.builder()
            .preProductionDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .productionDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .postDeploymentVerificationJobInstanceSummaries(new ArrayList<>())
            .build();
    List<String> verificationJobInstanceIds = getVerificationJobInstanceIds(deploymentActivities);
    verificationJobInstanceService.addResultsToDeploymentResultSummary(
        accountId, verificationJobInstanceIds, deploymentResultSummary);

    String serviceName = getServiceNameFromActivity(deploymentActivities.get(0));

    DeploymentActivityResultDTO deploymentActivityResultDTO = DeploymentActivityResultDTO.builder()
                                                                  .deploymentTag(deploymentTag)
                                                                  .serviceName(serviceName)
                                                                  .deploymentResultSummary(deploymentResultSummary)
                                                                  .build();

    Set<String> environments = collectAllEnvironments(deploymentActivityResultDTO);
    deploymentActivityResultDTO.setEnvironments(environments);

    return deploymentActivityResultDTO;
  }

  @Override
  public DeploymentActivityPopoverResultDTO getDeploymentActivityVerificationsPopoverSummary(String accountId,
      String orgIdentifier, String projectIdentifier, String serviceIdentifier, String deploymentTag) {
    List<DeploymentActivity> deploymentActivities =
        getDeploymentActivitiesByTag(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deploymentTag);
    List<String> verificationJobInstanceIds = getVerificationJobInstanceIds(deploymentActivities);
    DeploymentActivityPopoverResultDTO deploymentActivityPopoverResultDTO =
        verificationJobInstanceService.getDeploymentVerificationPopoverResult(verificationJobInstanceIds);
    deploymentActivityPopoverResultDTO.setTag(deploymentTag);
    deploymentActivityPopoverResultDTO.setServiceName(deploymentTag);
    deploymentActivityPopoverResultDTO.setServiceName(getServiceNameFromActivity(deploymentActivities.get(0)));
    return deploymentActivityPopoverResultDTO;
  }

  private Set<String> collectAllEnvironments(DeploymentActivityResultDTO deploymentActivityResultDTO) {
    Set<String> environments = new HashSet<>();
    deploymentActivityResultDTO.getDeploymentResultSummary()
        .getPreProductionDeploymentVerificationJobInstanceSummaries()
        .forEach(deploymentVerificationJobInstanceSummary
            -> environments.add(deploymentVerificationJobInstanceSummary.getEnvironmentName()));

    deploymentActivityResultDTO.getDeploymentResultSummary()
        .getProductionDeploymentVerificationJobInstanceSummaries()
        .forEach(deploymentVerificationJobInstanceSummary
            -> environments.add(deploymentVerificationJobInstanceSummary.getEnvironmentName()));

    deploymentActivityResultDTO.getDeploymentResultSummary()
        .getPostDeploymentVerificationJobInstanceSummaries()
        .forEach(deploymentVerificationJobInstanceSummary
            -> environments.add(deploymentVerificationJobInstanceSummary.getEnvironmentName()));

    return environments;
  }

  private List<String> getVerificationJobInstanceIds(List<DeploymentActivity> deploymentActivities) {
    return deploymentActivities.stream()
        .map(deploymentActivity -> deploymentActivity.getVerificationJobInstanceIds())
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
  private List<DeploymentActivity> getDeploymentActivitiesByTag(String accountId, String orgIdentifier,
      String projectIdentifier, String serviceIdentifier, String deploymentTag) {
    List<DeploymentActivity> deploymentActivities =
        (List<DeploymentActivity>) (List<?>) hPersistence
            .createQuery(Activity.class, Collections.singleton(HQuery.QueryChecks.COUNT))
            .filter(ActivityKeys.accountIdentifier, accountId)
            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
            .filter(ActivityKeys.serviceIdentifier, serviceIdentifier)
            .filter(ActivityKeys.type, ActivityType.DEPLOYMENT)
            .filter(DeploymentActivityKeys.deploymentTag, deploymentTag)
            .asList();
    Preconditions.checkState(
        isNotEmpty(deploymentActivities), "No Deployment Activities were found for deployment tag: %s", deploymentTag);
    return deploymentActivities;
  }

  private List<DeploymentGroupByTag> getRecentDeploymentActivities(
      String accountId, String orgIdentifier, String projectIdentifier) {
    List<DeploymentActivity> activities =
        (List<DeploymentActivity>) (List<?>) hPersistence.createQuery(Activity.class, excludeAuthority)
            .filter(ActivityKeys.accountIdentifier, accountId)
            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
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

  @Override
  public String getDeploymentTagFromActivity(String accountId, String verificationJobInstanceId) {
    DeploymentActivity deploymentActivity =
        (DeploymentActivity) hPersistence.createQuery(Activity.class, excludeAuthority)
            .filter(ActivityKeys.accountIdentifier, accountId)
            .filter(ActivityKeys.type, ActivityType.DEPLOYMENT)
            .field(ActivityKeys.verificationJobInstanceIds)
            .hasThisOne(verificationJobInstanceId)
            .get();
    if (deploymentActivity != null) {
      return deploymentActivity.getDeploymentTag();
    } else {
      throw new IllegalStateException("Activity not found for verificationJobInstanceId: " + verificationJobInstanceId);
    }
  }

  private String getServiceNameFromActivity(Activity activity) {
    return nextGenService
        .getService(activity.getServiceIdentifier(), activity.getAccountIdentifier(), activity.getOrgIdentifier(),
            activity.getProjectIdentifier())
        .getName();
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

  public Activity getActivityFromDTO(ActivityDTO activityDTO) {
    Activity activity;
    switch (activityDTO.getType()) {
      case DEPLOYMENT:
        activity = DeploymentActivity.builder().build();
        break;
      case INFRASTRUCTURE:
        activity = KubernetesActivity.builder().build();
        break;
      case CUSTOM:
        activity = CustomActivity.builder().build();
        break;
      default:
        throw new IllegalStateException("Invalid type " + activityDTO.getType());
    }

    activity.fromDTO(activityDTO);
    return activity;
  }
}
