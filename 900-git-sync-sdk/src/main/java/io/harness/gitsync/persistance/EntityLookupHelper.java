package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

  private String getKey(EntityScopeInfo entityScopeInfo) {
    String scope = "";
    if (!StringUtils.isEmpty(entityScopeInfo.getAccountId())) {
      scope += "/" + entityScopeInfo.getAccountId();
    }
    if (!StringUtils.isEmpty(entityScopeInfo.getOrgId().getValue())) {
      scope += "/" + entityScopeInfo.getOrgId().getValue();
    }

    if (!StringUtils.isEmpty(entityScopeInfo.getProjectId().getValue())) {
      scope += "/" + entityScopeInfo.getProjectId().getValue();
    }
    return scope;
  }
  @Override
  public boolean fetchKey(EntityScopeInfo entityScopeInfo) {
    final String scope = getKey(entityScopeInfo);
    Boolean isGitEnabled = gitEnabledCache.get(scope);
    if (isGitEnabled != null) {
      return isGitEnabled;
    } else {
      final Boolean gitSyncEnabled =
          GitSyncGrpcClientUtils
              .retryAndProcessException(
                  harnessToGitPushInfoServiceBlockingStub::isGitSyncEnabledForScope, entityScopeInfo)
              .getEnabled();
      gitEnabledCache.put(scope, gitSyncEnabled);
      return gitSyncEnabled;
    }
  }

  @Override
  public void updateKey(EntityScopeInfo entityScopeInfo) {
    log.info("Invalidating cache {}", entityScopeInfo);
    gitEnabledCache.remove(getKey(entityScopeInfo));
  }
}
