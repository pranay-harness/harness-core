package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceUpdatableEntity;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.entities.CDNGActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.UpdatableEntity;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class ActivitySourceServiceImpl implements ActivitySourceService {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private Injector injector;

  @Override
  public String create(String accountId, ActivitySourceDTO activitySourceDTO) {
    Preconditions.checkNotNull(activitySourceDTO.getOrgIdentifier());
    Preconditions.checkNotNull(activitySourceDTO.getProjectIdentifier());
    ActivitySource activitySource;
    switch (activitySourceDTO.getType()) {
      case KUBERNETES:
        activitySource = KubernetesActivitySource.fromDTO(accountId, activitySourceDTO.getOrgIdentifier(),
            activitySourceDTO.getProjectIdentifier(), (KubernetesActivitySourceDTO) activitySourceDTO);
        break;
      case HARNESS_CD10:
        validateSingleCD10Activity(
            accountId, activitySourceDTO.getOrgIdentifier(), activitySourceDTO.getProjectIdentifier());
        activitySource = CD10ActivitySource.fromDTO(accountId, activitySourceDTO.getOrgIdentifier(),
            activitySourceDTO.getProjectIdentifier(), (CD10ActivitySourceDTO) activitySourceDTO);
        break;
      case CDNG:
        throw new IllegalStateException("CDNG activity can not be created using the API.");
      default:
        throw new IllegalStateException("Invalid type " + activitySourceDTO.getType());
    }
    try {
      activitySource.validate();
      return hPersistence.save(activitySource);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format(
              "An Activity Source with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
              activitySource.getIdentifier(), activitySource.getOrgIdentifier(), activitySource.getProjectIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public String update(String accountId, String identifier, ActivitySourceDTO activitySourceDTO) {
    ActivitySource activitySource =
        get(accountId, activitySourceDTO.getOrgIdentifier(), activitySourceDTO.getProjectIdentifier(), identifier);
    if (activitySource == null) {
      throw new InvalidRequestException(
          String.format(
              "Activity Source with identifier [%s] , orgIdentifier [%s] and projectIdentifier [%s] not found",
              identifier, activitySourceDTO.getOrgIdentifier(), activitySourceDTO.getProjectIdentifier()),
          USER);
    }

    if (isNotEmpty(activitySource.getDataCollectionTaskId())) {
      verificationManagerService.deletePerpetualTask(
          activitySource.getAccountId(), activitySource.getDataCollectionTaskId());
    }

    UpdateOperations<ActivitySource> updateOperations = hPersistence.createUpdateOperations(ActivitySource.class);

    UpdatableEntity<ActivitySource, ActivitySourceDTO> updatableEntity = injector.getInstance(
        Key.get(ActivitySourceUpdatableEntity.class, Names.named(activitySource.getType().name())));
    updatableEntity.setUpdateOperations(updateOperations, activitySourceDTO);

    hPersistence.update(activitySource, updateOperations);
    return identifier;
  }

  private void validateSingleCD10Activity(String accountId, String orgIdentifier, String projectIdentifier) {
    CD10ActivitySource cd10ActivitySource = hPersistence.createQuery(CD10ActivitySource.class)
                                                .filter(ActivitySourceKeys.accountId, accountId)
                                                .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
                                                .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
                                                .filter(ActivitySourceKeys.type, ActivitySourceType.HARNESS_CD10)
                                                .get();
    if (cd10ActivitySource != null) {
      throw new IllegalStateException("There can only be one CD 1.0 activity source per project");
    }
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

  @Override
  public void createDefaultCDNGActivitySource(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ActivitySource activitySource =
        CDNGActivitySource.getDefaultObject(accountIdentifier, orgIdentifier, projectIdentifier);
    try {
      activitySource.validate();
      hPersistence.save(activitySource);
    } catch (DuplicateKeyException ex) {
      // This call is idempotent so ignoring the exception.
      log.info("Tried to create already existing CDNG activity source ");
    }
  }

  private boolean getActivitySourceForDeletion(String accountId, ActivitySource activitySource) {
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

  private ActivitySource get(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    Preconditions.checkNotNull(accountId);
    Preconditions.checkNotNull(orgIdentifier);
    Preconditions.checkNotNull(projectIdentifier);
    Preconditions.checkNotNull(identifier);
    return hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
        .filter(ActivitySourceKeys.accountId, accountId)
        .filter(ActivitySourceKeys.orgIdentifier, orgIdentifier)
        .filter(ActivitySourceKeys.projectIdentifier, projectIdentifier)
        .filter(ActivitySourceKeys.identifier, identifier)
        .get();
  }
}
