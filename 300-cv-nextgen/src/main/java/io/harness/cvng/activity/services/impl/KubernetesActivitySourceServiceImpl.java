package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource.KubernetesActivitySourceKeys;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DataCollectionTask.DataCollectionTaskKeys;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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
  public List<KubernetesActivitySourceDTO> listKubernetesSources(
      String accountId, String orgIdentifier, String projectIdentifier) {
    List<KubernetesActivitySource> kubernetesActivitySources =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
            .filter(KubernetesActivitySourceKeys.accountId, accountId)
            .filter(KubernetesActivitySourceKeys.orgIdentifier, orgIdentifier)
            .filter(KubernetesActivitySourceKeys.projectIdentifier, projectIdentifier)
            .asList();
    return kubernetesActivitySources.stream()
        .map(kubernetesActivitySource -> kubernetesActivitySource.toDTO())
        .collect(Collectors.toList());
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
      activity.setAccountIdentifier(accountId);
      activity.setOrgIdentifier(activitySource.getOrgIdentifier());
      activity.setProjectIdentifier(activitySource.getProjectIdentifier());
    });

    List<Activity> kubernetesActivities =
        activities.stream()
            .map(kubernetesActivityDTO -> activityService.getActivityFromDTO(kubernetesActivityDTO))
            .collect(Collectors.toList());
    hPersistence.save(kubernetesActivities);
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
