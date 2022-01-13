/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class ActivitySourceServiceImpl implements DeleteEntityByHandler<ActivitySource> {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationManagerService verificationManagerService;

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
