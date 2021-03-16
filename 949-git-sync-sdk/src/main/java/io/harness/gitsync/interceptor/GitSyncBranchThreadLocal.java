package io.harness.gitsync.interceptor;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GitSyncBranchThreadLocal {
  public static class Guard implements AutoCloseable {
    private GitBranchInfo old;
    Guard(GitBranchInfo gitBranchInfo) {
      old = get();
      set(gitBranchInfo);
    }

    @Override
    public void close() {
      set(old);
    }
  }

  public static Guard gitBranchGuard(GitBranchInfo gitBranchInfo) {
    return new Guard(gitBranchInfo);
  }

  public static final ThreadLocal<GitBranchInfo> GitSyncThreadLocal = new ThreadLocal<>();

  public static void set(GitBranchInfo gitBranchInfo) {
    GitSyncThreadLocal.set(gitBranchInfo);
  }

  public static void unset() {
    GitSyncThreadLocal.remove();
  }

  public static GitBranchInfo get() {
    return GitSyncThreadLocal.get();
  }
}
