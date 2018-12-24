package io.harness.limits.checker;

import io.harness.limits.lib.Limit;
import lombok.Value;

@Value
public class UsageLimitExceededException extends RuntimeException {
  private Limit limit;
  private String accountId;

  public UsageLimitExceededException(Limit limit, String accountId) {
    super("Usage limit reached. Limit: " + limit + " , accountId=" + accountId);
    this.limit = limit;
    this.accountId = accountId;
  }

  public String getMessage() {
    return "Usage limit reached. Limit: " + limit + " , accountId=" + accountId;
  }
}
