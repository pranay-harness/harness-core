package software.wings.logcontext;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;
import software.wings.beans.User;

public class UserLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(User.class);

  public UserLogContext(String accountId, String userId, OverrideBehavior behavior) {
    super(ImmutableMap.of(AccountLogContext.ID, accountId, ID, userId), behavior);
  }
}
