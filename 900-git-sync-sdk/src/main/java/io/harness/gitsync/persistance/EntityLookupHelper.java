/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

@ParametersAreNonnullByDefault
@Singleton
@Slf4j
@OwnedBy(DX)
public class EntityLookupHelper implements EntityKeySource {
  private final @NonNull Cache<String, Boolean> gitEnabledCache;
  HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;

  @Inject
  public EntityLookupHelper(HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub,
      @Named("gitSyncEnabledCache") Cache<String, Boolean> gitEnabledCache) {
    this.gitEnabledCache = gitEnabledCache;
    this.harnessToGitPushInfoServiceBlockingStub = harnessToGitPushInfoServiceBlockingStub;
  }

  @Override
  public boolean fetchKey(EntityScopeInfo entityScopeInfo) {
    final String scope = entityScopeInfo.toString();
    Boolean isGitEnabled = gitEnabledCache.get(scope);
    if (isGitEnabled != null) {
      return isGitEnabled;
    } else {
      final Boolean gitSyncEnabled =
          harnessToGitPushInfoServiceBlockingStub.isGitSyncEnabledForScope(entityScopeInfo).getEnabled();
      gitEnabledCache.put(scope, gitSyncEnabled);
      return gitSyncEnabled;
    }
  }

  @Override
  public void updateKey(EntityScopeInfo entityScopeInfo) {
    log.info("Invalidating cache {}", entityScopeInfo);
    gitEnabledCache.remove(entityScopeInfo.toString());
  }
}
