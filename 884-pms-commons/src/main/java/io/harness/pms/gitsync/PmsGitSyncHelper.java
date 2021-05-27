package io.harness.pms.gitsync;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PmsGitSyncHelper {
  @Inject private KryoSerializer kryoSerializer;

  public EntityGitDetails getEntityGitDetailsFromBytes(ByteString gitSyncBranchContextBytes) {
    GitSyncBranchContext gitSyncBranchContext = deserializeGitSyncBranchContext(gitSyncBranchContextBytes);
    if (gitSyncBranchContext == null) {
      return null;
    }
    return gitSyncBranchContext.toEntityGitDetails();
  }

  public ByteString getGitSyncBranchContextBytesThreadLocal() {
    GitSyncBranchContext gitSyncBranchContext = GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    return serializeGitSyncBranchContext(gitSyncBranchContext);
  }

  public PmsGitSyncBranchContextGuard createGitSyncBranchContextGuard(
      Ambiance ambiance, boolean findDefaultFromOtherBranches) {
    return createGitSyncBranchContextGuardFromBytes(
        ambiance.getMetadata().getGitSyncBranchContext(), findDefaultFromOtherBranches);
  }

  public PmsGitSyncBranchContextGuard createGitSyncBranchContextGuardFromBytes(
      ByteString gitSyncBranchContextBytes, boolean findDefaultFromOtherBranches) {
    return new PmsGitSyncBranchContextGuard(
        deserializeGitSyncBranchContext(gitSyncBranchContextBytes), findDefaultFromOtherBranches);
  }

  public GitSyncBranchContext deserializeGitSyncBranchContext(ByteString byteString) {
    if (isEmpty(byteString)) {
      return null;
    }
    byte[] bytes = byteString.toByteArray();
    return isEmpty(bytes) ? null : (GitSyncBranchContext) kryoSerializer.asInflatedObject(bytes);
  }

  public ByteString serializeGitSyncBranchContext(GitSyncBranchContext gitSyncBranchContext) {
    if (gitSyncBranchContext == null) {
      return null;
    }
    return ByteString.copyFrom(kryoSerializer.asDeflatedBytes(gitSyncBranchContext));
  }
}
