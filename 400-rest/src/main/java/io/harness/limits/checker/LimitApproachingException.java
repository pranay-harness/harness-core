package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.slf4j.helpers.MessageFormatter.arrayFormat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.limits.lib.Limit;

import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(PL)
@Value
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._980_COMMONS)
public class LimitApproachingException extends RuntimeException {
  private static final String ERROR_MSG = "{}% of Usage Limit Reached. limit={}, accountId={}";

  private Limit limit;
  private String accountId;
  private int percent;

  public LimitApproachingException(Limit limit, String accountId, int percent) {
    super(arrayFormat(ERROR_MSG, new Object[] {String.valueOf(percent), limit.toString(), accountId}).getMessage());
    this.limit = limit;
    this.accountId = accountId;
    this.percent = percent;
  }

  @Override
  public String getMessage() {
    return arrayFormat(ERROR_MSG, new Object[] {String.valueOf(percent), limit.toString(), accountId}).getMessage();
  }
}
