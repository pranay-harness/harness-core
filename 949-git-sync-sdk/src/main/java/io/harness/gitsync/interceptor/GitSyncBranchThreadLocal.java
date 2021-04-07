package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitSyncBranchThreadLocal {
  public static class Guard implements AutoCloseable {
    private GitEntityInfo old;

    Guard(GitEntityInfo gitBranchInfo) {
      old = get();
      set(gitBranchInfo);
    }

    @Override
    public void close() {
      set(old);
    }
  }

  public static Guard gitBranchGuard(GitEntityInfo gitBranchInfo) {
    return new Guard(gitBranchInfo);
  }

  public static final ThreadLocal<GitEntityInfo> GitSyncThreadLocal = new ThreadLocal<>();

  public static void set(GitEntityInfo gitBranchInfo) {
    GitSyncThreadLocal.set(gitBranchInfo);
  }

  public static void unset() {
    GitSyncThreadLocal.remove();
  }

  public static GitEntityInfo get() {
    return GitSyncThreadLocal.get();
  }
}
