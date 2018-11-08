package software.wings.service.impl.yaml.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.concurrent.TimeUnit;

@Singleton
public class FailedCommitStore {
  private final Cache<Commit, Boolean> commitsWhichExceedLimit =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

  public boolean didExceedLimit(Commit commit) {
    return commitsWhichExceedLimit.getIfPresent(commit) != null;
  }

  /**
   * This method should be called with a Commit Id  which lead to {@link
   * io.harness.eraro.ErrorCode#USAGE_LIMITS_EXCEEDED} error.
   */
  public void exceededLimit(Commit commit) {
    commitsWhichExceedLimit.put(commit, true);
  }

  @Value
  @AllArgsConstructor
  public static class Commit {
    String commitId;
    String accountId;
  }
}
