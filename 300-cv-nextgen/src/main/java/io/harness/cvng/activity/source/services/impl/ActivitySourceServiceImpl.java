package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ActivitySourceServiceImpl implements ActivitySourceService {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private CVEventService cvEventService;

  @Override
  public String saveActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, ActivitySourceDTO activitySourceDTO) {
    if (isNotEmpty(activitySourceDTO.getUuid())) {
      update(activitySourceDTO);
    }

    ActivitySource activitySource;
    switch (activitySourceDTO.getType()) {
      case KUBERNETES:
        activitySource = KubernetesActivitySource.fromDTO(
            accountId, orgIdentifier, projectIdentifier, (KubernetesActivitySourceDTO) activitySourceDTO);
        sendKubernetesActivitySourceCreateEvent((KubernetesActivitySource) activitySource);
        break;
      case HARNESS_CD10:
        activitySource = CD10ActivitySource.fromDTO(
            accountId, orgIdentifier, projectIdentifier, (CD10ActivitySourceDTO) activitySourceDTO);
        break;
      default:
        throw new IllegalStateException("Invalid type " + activitySourceDTO.getType());
    }
    activitySource.validate();
    return hPersistence.save(activitySource);
  }

  private void sendKubernetesActivitySourceCreateEvent(KubernetesActivitySource activitySource) {
    cvEventService.sendKubernetesActivitySourceConnectorCreateEvent(activitySource);
    cvEventService.sendKubernetesActivitySourceServiceCreateEvent(activitySource);
    cvEventService.sendKubernetesActivitySourceEnvironmentCreateEvent(activitySource);
  }

  private void update(ActivitySourceDTO activitySourceDTO) {
    ActivitySource activitySource = hPersistence.get(ActivitySource.class, activitySourceDTO.getUuid());
    if (isNotEmpty(activitySource.getDataCollectionTaskId())) {
      verificationManagerService.deletePerpetualTask(
          activitySource.getAccountId(), activitySource.getDataCollectionTaskId());
    }
    UpdateOperations<ActivitySource> updateOperations = hPersistence.createUpdateOperations(ActivitySource.class)
                                                            .set(ActivitySourceKeys.name, activitySourceDTO.getName())

                                                            .unset(ActivitySourceKeys.dataCollectionTaskId);

    switch (activitySourceDTO.getType()) {
      case KUBERNETES:
        KubernetesActivitySource.setUpdateOperations(updateOperations, (KubernetesActivitySourceDTO) activitySourceDTO);
        break;
      case HARNESS_CD10:
        CD10ActivitySource.setUpdateOperations(updateOperations, (CD10ActivitySourceDTO) activitySourceDTO);
        break;
      default:
        throw new IllegalStateException("Invalid type " + activitySourceDTO.getType());
    }
    hPersistence.update(hPersistence.get(ActivitySource.class, activitySourceDTO.getUuid()), updateOperations);
  }

  @Override
  public ActivitySource getActivitySource(String activitySourceId) {
    return hPersistence.get(ActivitySource.class, activitySourceId);
  }

  @Override
  public ActivitySourceDTO getActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    ActivitySource activitySource = hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
                                        .filter(ActivitySourceKeys.accountId, accountId)
                                        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                        .filter(ActivitySourceKeys.identifier, identifier)
                                        .get();
    if (activitySource == null) {
      return null;
    }
    return activitySource.toDTO();
  }

  @Override
  public PageResponse<ActivitySourceDTO> listActivitySources(
      String accountId, String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter) {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                               .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                               .asList();
    List<ActivitySourceDTO> activitySourceDTOs =
        activitySources.stream()
            .filter(activitySource
                -> isEmpty(filter) || activitySource.getName().toLowerCase().contains(filter.trim().toLowerCase()))
            .map(activitySource -> activitySource.toDTO())
            .collect(Collectors.toList());
    return PageUtils.offsetAndLimit(activitySourceDTOs, offset, pageSize);
  }

  @Override
  public boolean deleteActivitySource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    ActivitySource activitySource = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                        .filter(ActivitySourceKeys.accountId, accountId)
                                        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                        .filter(ActivitySourceKeys.identifier, identifier)
                                        .get();

    return getActivitySourceForDeletion(accountId, activitySource);
  }

  private boolean getActivitySourceForDeletion(String accountId, ActivitySource activitySource) {
    if (activitySource != null) {
      if (isNotEmpty(activitySource.getDataCollectionTaskId())) {
        verificationManagerService.deletePerpetualTask(accountId, activitySource.getDataCollectionTaskId());
      }
    }
    sendKubernetesActivitySourceDeleteEvent((KubernetesActivitySource) activitySource);
    return hPersistence.delete(activitySource);
  }

  private void sendKubernetesActivitySourceDeleteEvent(KubernetesActivitySource activitySource) {
    cvEventService.sendKubernetesActivitySourceConnectorDeleteEvent(activitySource);
    cvEventService.sendKubernetesActivitySourceServiceDeleteEvent(activitySource);
    cvEventService.sendKubernetesActivitySourceEnvironmentDeleteEvent(activitySource);
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<ActivitySource> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                               .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                               .asList();

    for (ActivitySource activitySource : activitySources) {
      getActivitySourceForDeletion(activitySource.getAccountId(), activitySource);
    }
  }

  @Override
  public void deleteByOrgIdentifier(Class<ActivitySource> clazz, String accountId, String orgIdentifier) {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                               .asList();

    for (ActivitySource activitySource : activitySources) {
      getActivitySourceForDeletion(activitySource.getAccountId(), activitySource);
    }
  }

  @Override
  public void deleteByAccountIdentifier(Class<ActivitySource> clazz, String accountId) {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .asList();
    for (ActivitySource activitySource : activitySources) {
      getActivitySourceForDeletion(activitySource.getAccountId(), activitySource);
    }
  }
}
