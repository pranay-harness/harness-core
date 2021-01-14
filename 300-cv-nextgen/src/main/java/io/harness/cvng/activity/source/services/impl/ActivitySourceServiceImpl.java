package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.client.VerificationManagerService;
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
  private static final int RECENT_DEPLOYMENT_ACTIVITIES_RESULT_SIZE = 5;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationManagerService verificationManagerService;

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
        break;
      default:
        throw new IllegalStateException("Invalid type " + activitySourceDTO.getType());
    }
    return hPersistence.save(activitySource);
  }

  private void update(ActivitySourceDTO activitySourceDTO) {
    KubernetesActivitySource kubernetesActivitySource =
        hPersistence.get(KubernetesActivitySource.class, activitySourceDTO.getUuid());
    if (isNotEmpty(kubernetesActivitySource.getDataCollectionTaskId())) {
      verificationManagerService.deletePerpetualTask(
          kubernetesActivitySource.getAccountId(), kubernetesActivitySource.getDataCollectionTaskId());
    }
    UpdateOperations<ActivitySource> updateOperations = hPersistence.createUpdateOperations(ActivitySource.class)
                                                            .set(ActivitySourceKeys.name, activitySourceDTO.getName())

                                                            .unset(ActivitySourceKeys.dataCollectionTaskId);

    switch (activitySourceDTO.getType()) {
      case KUBERNETES:
        KubernetesActivitySource.setUpdateOperations(updateOperations, (KubernetesActivitySourceDTO) activitySourceDTO);
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
    if (activitySource != null) {
      if (isNotEmpty(activitySource.getDataCollectionTaskId())) {
        verificationManagerService.deletePerpetualTask(accountId, activitySource.getDataCollectionTaskId());
      }
    }
    return hPersistence.delete(activitySource);
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<ActivitySource> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    List<ActivitySource> activitySources = hPersistence.createQuery(ActivitySource.class, excludeAuthority)
                                               .filter(ActivitySourceKeys.accountId, accountId)
                                               .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                               .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                               .asList();
    activitySources.forEach(activitySource
        -> deleteActivitySource(accountId, orgIdentifier, projectIdentifier, activitySource.getIdentifier()));
  }
}
