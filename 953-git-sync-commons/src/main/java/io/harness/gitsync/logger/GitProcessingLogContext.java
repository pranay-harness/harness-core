package io.harness.gitsync.logger;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.data.structure.NullSafeImmutableMap.NullSafeBuilder;
import io.harness.logging.AutoLogContext;

import java.util.Map;

public class GitProcessingLogContext extends AutoLogContext {
  public static String COMMIT_ID = "commitId";

  private static Map<String, String> getContext(String accountId, String commitId) {
    NullSafeBuilder<String, String> nullSafeBuilder = NullSafeImmutableMap.builder();
    nullSafeBuilder.putIfNotNull(COMMIT_ID, commitId);
    nullSafeBuilder.putIfNotNull(ACCOUNT_KEY, accountId);
    return nullSafeBuilder.build();
  }

  public GitProcessingLogContext(String accountId, String commitId, OverrideBehavior behavior) {
    super(getContext(accountId, commitId), behavior);
  }
}
