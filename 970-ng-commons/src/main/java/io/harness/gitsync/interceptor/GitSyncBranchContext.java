package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(DX)
@TypeAlias("GitSyncBranchContext")
public class GitSyncBranchContext implements GlobalContextData {
  public static final String NG_GIT_SYNC_CONTEXT = "NG_GIT_SYNC_CONTEXT";

  GitEntityInfo gitBranchInfo;

  @Override
  public String getKey() {
    return NG_GIT_SYNC_CONTEXT;
  }
}
