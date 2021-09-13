package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO;
import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO.KubernetesActivityDetail;
import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO.KubernetesActivityDetailsDTOBuilder;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivity.KubernetesActivityKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource.KubernetesActivitySourceKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.encryption.Scope;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.time.Timestamp;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class KubernetesActivitySourceServiceImpl implements KubernetesActivitySourceService {
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private ChangeSourceService changeSourceService;

  @Override
  public boolean saveKubernetesActivities(
      String accountId, String activitySourceId, List<KubernetesActivityDTO> activities) {
    ActivitySource activitySource = activitySourceService.getActivitySource(activitySourceId);
    Preconditions.checkNotNull(activitySource, "No source found with {}", activitySourceId);

    activities.forEach(activity -> {
      Preconditions.checkNotNull(activity.getEnvironmentIdentifier(), "environment identifier can not be null");
      Preconditions.checkNotNull(activity.getServiceIdentifier(), "service identifier can not be null");
      Instant bucketStartTime = Instant.ofEpochMilli(Timestamp.minuteBoundary(activity.getActivityStartTime()));
      Query<Activity> query = hPersistence.createQuery(Activity.class, excludeAuthority)
                                  .disableValidation()
                                  .filter(ActivityKeys.accountId, accountId)
                                  .filter(ActivityKeys.orgIdentifier, activitySource.getOrgIdentifier())
                                  .filter(ActivityKeys.projectIdentifier, activitySource.getProjectIdentifier())
                                  .filter(ActivityKeys.activitySourceId, activitySourceId)
                                  .filter(KubernetesActivityKeys.namespace, activity.getNamespace())
                                  .filter(KubernetesActivityKeys.workloadName, activity.getWorkloadName())
                                  .filter(KubernetesActivityKeys.kind, activity.getKind())
                                  .filter(KubernetesActivityKeys.bucketStartTime, bucketStartTime);

      Activity savedActivity = hPersistence.upsert(query,
          hPersistence.createUpdateOperations(Activity.class)
              .disableValidation()
              .setOnInsert("className", KubernetesActivity.class.getName())
              .setOnInsert(ActivityKeys.accountId, accountId)
              .setOnInsert(ActivityKeys.uuid, generateUuid())
              .setOnInsert(ActivityKeys.activitySourceId, activitySourceId)
              .setOnInsert(ActivityKeys.orgIdentifier, activitySource.getOrgIdentifier())
              .setOnInsert(ActivityKeys.projectIdentifier, activitySource.getProjectIdentifier())
              .setOnInsert(ActivityKeys.environmentIdentifier, activity.getEnvironmentIdentifier())
              .setOnInsert(ActivityKeys.serviceIdentifier, activity.getServiceIdentifier())
              .setOnInsert(ActivityKeys.activityStartTime, bucketStartTime)
              .setOnInsert(ActivityKeys.type, ActivityType.KUBERNETES)
              .setOnInsert(ActivityKeys.validUntil, KubernetesActivity.builder().build().getValidUntil())
              .setOnInsert(KubernetesActivityKeys.namespace, activity.getNamespace())
              .setOnInsert(KubernetesActivityKeys.kind, activity.getKind())
              .setOnInsert(KubernetesActivityKeys.workloadName, activity.getWorkloadName())
              .setOnInsert(ActivityKeys.analysisStatus, ActivityVerificationStatus.NOT_STARTED)
              .setOnInsert(KubernetesActivityKeys.bucketStartTime, bucketStartTime)
              .addToSet(KubernetesActivityKeys.activities, activity),
          new FindAndModifyOptions().upsert(true));
      List<String> verificationJobInstances = activityService.createVerificationJobInstancesForActivity(savedActivity);
      if (isNotEmpty(verificationJobInstances)) {
        savedActivity.setVerificationJobInstanceIds(verificationJobInstances);
        hPersistence.save(savedActivity);
      }
    });
    return true;
  }

  @Override
  public boolean saveKubernetesChange(String accountId, KubernetesChangeEventDTO changeEventDTO) {
    KubernetesActivity activity = KubernetesActivity.builder()
                                      .accountId(changeEventDTO.getAccountId())
                                      .orgIdentifier(changeEventDTO.getOrgIdentifier())
                                      .projectIdentifier(changeEventDTO.getProjectIdentifier())
                                      .oldYaml(changeEventDTO.getOldYaml())
                                      .newYaml(changeEventDTO.getNewYaml())
                                      .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
                                      .build();
    hPersistence.save(activity);
    return true;
  }

  @Override
  public void enqueueDataCollectionTask(KubernetesActivitySource activitySource) {
    log.info("Enqueuing activitySourceId for the first time: {}", activitySource.getUuid());

    DataCollectionConnectorBundle dataCollectionConnectorBundle =
        DataCollectionConnectorBundle.builder()
            .dataCollectionType(DataCollectionType.KUBERNETES)
            .connectorIdentifier(activitySource.getConnectorIdentifier())
            .sourceIdentifier(activitySource.getIdentifier())
            .dataCollectionWorkerId(activitySource.getUuid())
            .build();
    String dataCollectionTaskId = verificationManagerService.createDataCollectionTask(activitySource.getAccountId(),
        activitySource.getOrgIdentifier(), activitySource.getProjectIdentifier(), dataCollectionConnectorBundle);

    UpdateOperations<KubernetesActivitySource> updateOperations =
        hPersistence.createUpdateOperations(KubernetesActivitySource.class)
            .set(ActivitySourceKeys.dataCollectionTaskId, dataCollectionTaskId);
    Query<KubernetesActivitySource> query = hPersistence.createQuery(KubernetesActivitySource.class)
                                                .filter(ActivitySourceKeys.uuid, activitySource.getUuid());
    hPersistence.update(query, updateOperations);

    log.info("Enqueued activity source successfully: {}", activitySource.getUuid());
  }

  @Override
  public boolean doesAActivitySourceExistsForThisProject(
      String accountId, String orgIdentifier, String projectIdentifier) {
    long numberOfActivitySources = hPersistence.createQuery(KubernetesActivitySource.class)
                                       .filter(ActivitySourceKeys.accountId, accountId)
                                       .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                       .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                       .count();
    return numberOfActivitySources > 0;
  }

  @Override
  public int getNumberOfKubernetesServicesSetup(String accountId, String orgIdentifier, String projectIdentifier) {
    BasicDBObject scopeIdentifiersFilter = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(ActivitySourceKeys.accountId, accountId));
    conditions.add(new BasicDBObject(ActivitySourceKeys.projectIdentifier, projectIdentifier));
    conditions.add(new BasicDBObject(ActivitySourceKeys.orgIdentifier, orgIdentifier));
    scopeIdentifiersFilter.put("$and", conditions);
    List<String> serviceIdentifiers =
        hPersistence.getCollection(KubernetesActivitySource.class)
            .distinct(KubernetesActivitySource.SERVICE_IDENTIFIER_KEY, scopeIdentifiersFilter);
    return serviceIdentifiers.size();
  }

  @Override
  public PageResponse<String> getKubernetesNamespaces(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, int offset, int pageSize, String filter) {
    List<String> kubernetesNamespaces = verificationManagerService.getKubernetesNamespaces(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, filter);
    return PageUtils.offsetAndLimit(kubernetesNamespaces, offset, pageSize);
  }

  @Override
  public PageResponse<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, int offset, int pageSize, String filter) {
    List<String> kubernetesWorkloads = verificationManagerService.getKubernetesWorkloads(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, namespace, filter);
    return PageUtils.offsetAndLimit(kubernetesWorkloads, offset, pageSize);
  }

  @Override
  public void resetLiveMonitoringPerpetualTaskForKubernetesActivitySource(
      KubernetesActivitySource kubernetesActivitySource) {
    DataCollectionConnectorBundle dataCollectionConnectorBundle =
        DataCollectionConnectorBundle.builder()
            .dataCollectionType(DataCollectionType.KUBERNETES)
            .connectorIdentifier(kubernetesActivitySource.getConnectorIdentifier())
            .sourceIdentifier(kubernetesActivitySource.getIdentifier())
            .build();

    verificationManagerService.resetDataCollectionTask(kubernetesActivitySource.getAccountId(),
        kubernetesActivitySource.getOrgIdentifier(), kubernetesActivitySource.getProjectIdentifier(),
        kubernetesActivitySource.getDataCollectionTaskId(), dataCollectionConnectorBundle);
  }

  @Override
  public List<KubernetesActivitySource> findByConnectorIdentifier(String accountId, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, String connectorIdentifierWithoutScopePrefix, Scope scope) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(connectorIdentifierWithoutScopePrefix);
    String connectorIdentifier = connectorIdentifierWithoutScopePrefix;
    if (scope == Scope.ACCOUNT || scope == Scope.ORG) {
      connectorIdentifier = scope.getYamlRepresentation() + "." + connectorIdentifierWithoutScopePrefix;
    }
    Query<KubernetesActivitySource> query =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
            .filter(ActivitySourceKeys.accountId, accountId)
            .filter(KubernetesActivitySourceKeys.connectorIdentifier, connectorIdentifier);
    if (scope == Scope.ORG) {
      query = query.filter(ActivitySourceKeys.orgIdentifier, orgIdentifier);
    }
    if (scope == Scope.PROJECT) {
      query = query.filter(ActivitySourceKeys.projectIdentifier, projectIdentifier);
    }
    return query.asList();
  }

  @Override
  public KubernetesActivityDetailsDTO getEventDetails(
      String accountId, String orgIdentifier, String projectIdentifier, String activityId) {
    Activity activity = hPersistence.createQuery(Activity.class, excludeAuthority)
                            .filter(ActivityKeys.accountId, accountId)
                            .filter(ActivityKeys.orgIdentifier, orgIdentifier)
                            .filter(ActivityKeys.projectIdentifier, projectIdentifier)
                            .filter(ActivityKeys.uuid, activityId)
                            .get();

    Preconditions.checkNotNull(activity, "No activity found with id %s", activityId);
    Preconditions.checkState(ActivityType.KUBERNETES.equals(activity.getType()),
        "activity with id %s is not a kubernetes activity", activityId);

    KubernetesActivitySource activitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(activity.getActivitySourceId());
    KubernetesActivity kubernetesActivity = (KubernetesActivity) activity;
    KubernetesActivityDetailsDTOBuilder kubernetesActivityDetailsDTOBuilder =
        KubernetesActivityDetailsDTO.builder()
            .namespace(kubernetesActivity.getNamespace())
            .kind(kubernetesActivity.getKind())
            .workload(kubernetesActivity.getWorkloadName())
            .sourceName(activitySource.getName())
            .connectorIdentifier(activitySource.getConnectorIdentifier());

    kubernetesActivity.getActivities().forEach(kubernetesActivityDTO
        -> kubernetesActivityDetailsDTOBuilder.detail(KubernetesActivityDetail.builder()
                                                          .timeStamp(kubernetesActivityDTO.getActivityStartTime())
                                                          .eventType(kubernetesActivityDTO.getEventType())
                                                          .reason(kubernetesActivityDTO.getReason())
                                                          .message(kubernetesActivityDTO.getMessage())
                                                          .eventJson(kubernetesActivityDTO.getEventJson())
                                                          .build()));
    return kubernetesActivityDetailsDTOBuilder.build();
  }

  @Override
  public void checkConnectivity(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String tracingId) {
    List<String> kubernetesNamespaces = verificationManagerService.getKubernetesNamespaces(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, null);
    if (!kubernetesNamespaces.isEmpty()) {
      verificationManagerService.getKubernetesWorkloads(
          accountId, orgIdentifier, projectIdentifier, connectorIdentifier, kubernetesNamespaces.get(0), null);
    }
    verificationManagerService.checkCapabilityToGetKubernetesEvents(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier);
  }
}
