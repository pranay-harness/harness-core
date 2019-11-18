package software.wings.logcontext;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class UserLogContext extends AutoLogContext {
  public UserLogContext(String accountId, String userId, OverrideBehavior behavior) {
    super(ImmutableMap.of("accountId", accountId, "userId", userId), behavior);
  }
}
