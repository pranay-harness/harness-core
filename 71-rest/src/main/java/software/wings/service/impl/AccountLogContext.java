package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AccountLogContext extends AutoLogContext {
  public static final String ID = "accountId";

  public AccountLogContext(String accountId) {
    super(ID, accountId);
  }
}
