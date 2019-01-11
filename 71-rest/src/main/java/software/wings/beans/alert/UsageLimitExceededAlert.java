package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.limits.lib.Limit;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Value
@AllArgsConstructor
public class UsageLimitExceededAlert implements AlertData {
  @Nonnull private String accountId;
  @Nullable private Limit limit;
  @Nonnull private String message;

  @Override
  public boolean matches(AlertData alertData) {
    UsageLimitExceededAlert alert = (UsageLimitExceededAlert) alertData;

    boolean limitSame;
    if (alert.getLimit() != null) {
      limitSame = alert.getLimit().equals(limit);
    } else {
      limitSame = alert.getLimit() == limit;
    }

    return alert.getAccountId().equals(accountId) && limitSame;
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
