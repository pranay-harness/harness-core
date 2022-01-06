/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
