package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.IsGitSyncEnabled;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import org.checkerframework.checker.nullness.qual.NonNull;

@ParametersAreNonnullByDefault
@Singleton
@OwnedBy(DX)
public class EntityLookupHelper implements EntityKeySource {
  private final @NonNull Cache<Object, Object> keyCache;
  HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  private final int SCOPE_GIT_SYNC_ENABLED_CACHE_TIME = 1 /*hour*/;
  private final int SCOPE_GIT_SYNC_ENABLED_CACHE_SIZE = 1000;

  @Inject
  public EntityLookupHelper(HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub) {
    this.keyCache = Caffeine.newBuilder()
                        .maximumSize(SCOPE_GIT_SYNC_ENABLED_CACHE_SIZE)
                        .expireAfterWrite(SCOPE_GIT_SYNC_ENABLED_CACHE_TIME, TimeUnit.HOURS)
                        .build();
    this.harnessToGitPushInfoServiceBlockingStub = harnessToGitPushInfoServiceBlockingStub;
  }

  @Override
  public boolean fetchKey(EntityScopeInfo entityScopeInfo) {
    return (boolean) keyCache.get(entityScopeInfo,
        ref -> harnessToGitPushInfoServiceBlockingStub.isGitSyncEnabledForScope(entityScopeInfo).getEnabled());
  }

  @Override
  public void updateKey(EntityScopeInfo entityScopeInfo) {
    final IsGitSyncEnabled gitSyncEnabledForScope =
        harnessToGitPushInfoServiceBlockingStub.isGitSyncEnabledForScope(entityScopeInfo);
    keyCache.put(entityScopeInfo, gitSyncEnabledForScope.getEnabled());
  }
}
