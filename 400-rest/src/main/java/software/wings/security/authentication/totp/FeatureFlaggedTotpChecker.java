package software.wings.security.authentication.totp;

import static io.harness.beans.FeatureName.RATE_LIMITED_TOTP;

import io.harness.ff.FeatureFlagService;

import software.wings.security.authentication.TotpChecker;

import lombok.EqualsAndHashCode;
import lombok.Getter;

public class FeatureFlaggedTotpChecker<T extends FeatureFlaggedTotpChecker.Request> implements TotpChecker<T> {
  private final TotpChecker<? super T> featureFlaggedChecker;
  private final TotpChecker<? super T> nonFeatureFlaggedChecker;
  private final FeatureFlagService featureFlagService;

  public FeatureFlaggedTotpChecker(TotpChecker<? super T> featureFlaggedChecker,
      TotpChecker<? super T> nonFeatureFlaggedChecker, FeatureFlagService featureFlagService) {
    this.featureFlaggedChecker = featureFlaggedChecker;
    this.nonFeatureFlaggedChecker = nonFeatureFlaggedChecker;
    this.featureFlagService = featureFlagService;
  }

  @Override
  public boolean check(T request) {
    if (featureFlagService.isEnabled(RATE_LIMITED_TOTP, request.getAccount())) {
      return featureFlaggedChecker.check(request);
    } else {
      return nonFeatureFlaggedChecker.check(request);
    }
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static class Request extends RateLimitedTotpChecker.Request {
    private final String account;

    public Request(String secret, int code, String userUuid, String userEmail, String account) {
      super(secret, code, userUuid, userEmail);
      this.account = account;
    }
  }
}
