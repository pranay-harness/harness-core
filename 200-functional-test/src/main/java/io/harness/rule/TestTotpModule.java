/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import software.wings.security.authentication.TotpChecker;
import software.wings.security.authentication.totp.FeatureFlaggedTotpChecker;
import software.wings.security.authentication.totp.SimpleTotpChecker;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class TestTotpModule extends AbstractModule {
  @Provides
  @Singleton
  @Named("featureFlagged")
  public TotpChecker<? super FeatureFlaggedTotpChecker.Request> checker() {
    return new SimpleTotpChecker<>();
  }
}
