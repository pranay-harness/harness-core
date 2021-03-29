package io.harness.gitsync.persistance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.common.EntityReference;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.IsGitSyncEnabled;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;
import org.checkerframework.checker.nullness.qual.NonNull;

@ParametersAreNonnullByDefault
@Singleton
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
  public boolean fetchKey(EntityReference entityReference) {
    return (boolean) keyCache.get(entityReference,
        ref
        -> harnessToGitPushInfoServiceBlockingStub
               .isGitSyncEnabledForScope(getEntityScopeFromEntityReference(entityReference))
               .getEnabled());
  }

  @Override
  public void updateKey(EntityReference entityReference) {
    final IsGitSyncEnabled gitSyncEnabledForScope = harnessToGitPushInfoServiceBlockingStub.isGitSyncEnabledForScope(
        getEntityScopeFromEntityReference(entityReference));
    keyCache.put(entityReference, gitSyncEnabledForScope.getEnabled());
  }

  private EntityScopeInfo getEntityScopeFromEntityReference(EntityReference entityReference) {
    final EntityScopeInfo.Builder entityScopeBuilder =
        EntityScopeInfo.newBuilder().setAccountId(entityReference.getAccountIdentifier());
    if (!isEmpty(entityReference.getOrgIdentifier())) {
      entityScopeBuilder.setOrgId(StringValue.of(entityReference.getOrgIdentifier()));
    }
    if (!isEmpty(entityReference.getProjectIdentifier())) {
      entityScopeBuilder.setProjectId(StringValue.of(entityReference.getProjectIdentifier()));
    }
    return entityScopeBuilder.build();
  }
}
