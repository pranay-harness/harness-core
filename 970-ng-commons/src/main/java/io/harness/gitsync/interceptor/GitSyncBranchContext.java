package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.gitsync.sdk.EntityGitDetails;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(DX)
@TypeAlias("GitSyncBranchContext")
@ToString
public class GitSyncBranchContext implements GlobalContextData {
  public static final String NG_GIT_SYNC_CONTEXT = "NG_GIT_SYNC_CONTEXT";

  @Wither GitEntityInfo gitBranchInfo;

  @Override
  public String getKey() {
    return NG_GIT_SYNC_CONTEXT;
  }

  public EntityGitDetails toEntityGitDetails() {
    if (gitBranchInfo == null || gitBranchInfo.isNull()) {
      return null;
    }
    return gitBranchInfo.toEntityGitDetails();
  }
}
