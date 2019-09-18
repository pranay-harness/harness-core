package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AccountLogContext extends AutoLogContext {
  public AccountLogContext(String accountId) {
    super("accountId", accountId);
  }
}
