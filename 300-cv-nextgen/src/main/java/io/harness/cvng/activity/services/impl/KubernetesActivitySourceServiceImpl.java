package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.Activity.ActivityKeys;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivity.KubernetesActivityKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource.KubernetesActivitySourceKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.time.Timestamp;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class KubernetesActivitySourceServiceImpl implements KubernetesActivitySourceService {
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;

  @Override
  public KubernetesActivitySource getActivitySource(String activitySourceId) {
    return hPersistence.get(KubernetesActivitySource.class, activitySourceId);
  }

  @Override
  public String saveKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, KubernetesActivitySourceDTO activitySourceDTO) {
    if (isNotEmpty(activitySourceDTO.getUuid())) {
      update(activitySourceDTO);
    }
    return hPersistence.save(KubernetesActivitySource.builder()
                                 .uuid(activitySourceDTO.getUuid())
                                 .accountId(accountId)
                                 .orgIdentifier(orgIdentifier)
                                 .projectIdentifier(projectIdentifier)
                                 .uuid(activitySourceDTO.getUuid())
                                 .identifier(activitySourceDTO.getIdentifier())
                                 .name(activitySourceDTO.getName())
                                 .connectorIdentifier(activitySourceDTO.getConnectorIdentifier())
                                 .activitySourceConfigs(activitySourceDTO.getActivitySourceConfigs())
                                 .build());
  }

  private void update(KubernetesActivitySourceDTO activitySourceDTO) {
    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, activitySourceDTO.getUuid());
    if (isNotEmpty(kubernetesActivitySource.getDataCollectionTaskId())) {
      verificationManagerService.deletePerpetualTask(
          kubernetesActivitySource.getAccountId(), kubernetesActivitySource.getDataCollectionTaskId());
    }
    hPersistence.update(hPersistence.get(KubernetesActivitySource.class, activitySourceDTO.getUuid()),
        hPersistence.createUpdateOperations(KubernetesActivitySource.class)
            .set(KubernetesActivitySourceKeys.name, activitySourceDTO.getName())
            .set(KubernetesActivitySourceKeys.connectorIdentifier, activitySourceDTO.getConnectorIdentifier())
            .set(KubernetesActivitySourceKeys.activitySourceConfigs, activitySourceDTO.getActivitySourceConfigs())
            .unset(KubernetesActivitySourceKeys.dataCollectionTaskId));
  }

  @Override
  public KubernetesActivitySourceDTO getKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
            .filter(KubernetesActivitySourceKeys.accountId, accountId)
            .filter(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier)
            .filter(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier)
            .filter(KubernetesActivitySourceKeys.identifier, identifier)
            .get();
    if (kubernetesActivitySource == null) {
      return null;
    }
    return kubernetesActivitySource.toDTO();
  }

  @Override
  public PageResponse<KubernetesActivitySourceDTO> listKubernetesSources(
      String accountId, String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter) {
    List<KubernetesActivitySource> kubernetesActivitySources =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
            .filter(KubernetesActivitySourceKeys.accountId, accountId)
            .filter(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier)
            .filter(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier)
            .asList();
    List<KubernetesActivitySourceDTO> activitySourceDTOs =
        kubernetesActivitySources.stream()
            .filter(kubernetesActivitySource
                -> isEmpty(filter)
                    || kubernetesActivitySource.getName().toLowerCase().contains(filter.trim().toLowerCase()))
            .map(kubernetesActivitySource -> kubernetesActivitySource.toDTO())
            .collect(Collectors.toList());
    return PageUtils.offsetAndLimit(activitySourceDTOs, offset, pageSize);
  }

  @Override
  public boolean deleteKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return hPersistence.delete(hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
                                   .filter(KubernetesActivitySourceKeys.accountId, accountId)
                                   .filter(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier)
                                   .filter(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier)
                                   .filter(KubernetesActivitySourceKeys.identifier, identifier));
  }

  @Override
  public boolean saveKubernetesActivities(
      String accountId, String activitySourceId, List<KubernetesActivityDTO> activities) {
    KubernetesActivitySource activitySource = getActivitySource(activitySourceId);
    Preconditions.checkNotNull(activitySource, "No source found with {}", activitySourceId);

    activities.forEach(activity -> {
      Preconditions.checkNotNull(activity.getEnvironmentIdentifier(), "environment identifier can not be null");
      Preconditions.checkNotNull(activity.getServiceIdentifier(), "service identifier can not be null");
      Instant bucketStartTime = Instant.ofEpochMilli(Timestamp.minuteBoundary(activity.getActivityStartTime()));
      Query<Activity> query =
          hPersistence.createQuery(Activity.class, excludeAuthority)
              .disableValidation()
              .filter(ActivityKeys.accountIdentifier, accountId)
              .filter(ActivityKeys.orgIdentifier, activitySource.getOrgIdentifier())
              .filter(ActivityKeys.projectIdentifier, activitySource.getProjectIdentifier())
              .filter(KubernetesActivityKeys.kubernetesActivityType, activity.getKubernetesActivityType())
              .filter(KubernetesActivityKeys.eventType, activity.getEventType())
              .filter(KubernetesActivityKeys.bucketStartTime, bucketStartTime);

      Activity savedActivity = hPersistence.upsert(query,
          hPersistence.createUpdateOperations(Activity.class)
              .disableValidation()
              .setOnInsert("className", KubernetesActivity.class.getName())
              .setOnInsert(ActivityKeys.accountIdentifier, accountId)
              .setOnInsert(ActivityKeys.uuid, generateUuid())
              .setOnInsert(ActivityKeys.orgIdentifier, activitySource.getOrgIdentifier())
              .setOnInsert(ActivityKeys.projectIdentifier, activitySource.getProjectIdentifier())
              .setOnInsert(ActivityKeys.environmentIdentifier, activity.getEnvironmentIdentifier())
              .setOnInsert(ActivityKeys.serviceIdentifier, activity.getServiceIdentifier())
              .setOnInsert(ActivityKeys.activityStartTime, bucketStartTime)
              .setOnInsert(ActivityKeys.type, ActivityType.KUBERNETES)
              .setOnInsert(KubernetesActivityKeys.kubernetesActivityType, activity.getKubernetesActivityType())
              .setOnInsert(KubernetesActivityKeys.eventType, activity.getEventType())
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
  public void enqueueDataCollectionTask(KubernetesActivitySource activitySource) {
    log.info("Enqueuing activitySourceId for the first time: {}", activitySource.getUuid());

    Map<String, String> params = new HashMap<>();
    params.put(CVConfigKeys.connectorIdentifier, activitySource.getConnectorIdentifier());
    params.put(DataCollectionTaskKeys.dataCollectionWorkerId, activitySource.getUuid());
    DataCollectionConnectorBundle dataCollectionConnectorBundle = DataCollectionConnectorBundle.builder()
                                                                      .dataCollectionType(DataCollectionType.KUBERNETES)
                                                                      .params(params)
                                                                      .activitySourceDTO(activitySource.toDTO())
                                                                      .build();
    String dataCollectionTaskId = verificationManagerService.createDataCollectionTask(activitySource.getAccountId(),
        activitySource.getOrgIdentifier(), activitySource.getProjectIdentifier(), dataCollectionConnectorBundle);

    UpdateOperations<KubernetesActivitySource> updateOperations =
        hPersistence.createUpdateOperations(KubernetesActivitySource.class)
            .set(KubernetesActivitySourceKeys.dataCollectionTaskId, dataCollectionTaskId);
    Query<KubernetesActivitySource> query = hPersistence.createQuery(KubernetesActivitySource.class)
                                                .filter(KubernetesActivitySourceKeys.uuid, activitySource.getUuid());
    hPersistence.update(query, updateOperations);

    log.info("Enqueued activity source successfully: {}", activitySource.getUuid());
  }

  @Override
  public boolean doesAActivitySourceExistsForThisProject(
      String accountId, String orgIdentifier, String projectIdentifier) {
    long numberOfActivitySources = hPersistence.createQuery(KubernetesActivitySource.class)
                                       .filter(KubernetesActivitySourceKeys.accountId, accountId)
                                       .filter(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier)
                                       .filter(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier)
                                       .count();
    return numberOfActivitySources > 0;
  }

  @Override
  public int getNumberOfServicesSetup(String accountId, String orgIdentifier, String projectIdentifier) {
    BasicDBObject scopeIdentifiersFilter = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(KubernetesActivitySourceKeys.accountId, accountId));
    conditions.add(new BasicDBObject(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier));
    conditions.add(new BasicDBObject(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier));
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
}
