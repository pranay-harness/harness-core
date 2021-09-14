/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.data.structure.NullSafeImmutableMap.NullSafeBuilder;
import io.harness.logging.AutoLogContext;

import java.util.Map;

public class NgAutoLogContext extends AutoLogContext {
  private static Map<String, String> getContext(
      String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    NullSafeBuilder<String, String> nullSafeBuilder = NullSafeImmutableMap.builder();
    nullSafeBuilder.putIfNotNull(PROJECT_KEY, projectIdentifier);
    nullSafeBuilder.putIfNotNull(ORG_KEY, orgIdentifier);
    nullSafeBuilder.putIfNotNull(ACCOUNT_KEY, accountIdentifier);
    return nullSafeBuilder.build();
  }

  public NgAutoLogContext(
      String projectIdentifier, String orgIdentifier, String accountIdentifier, OverrideBehavior behavior) {
    super(getContext(projectIdentifier, orgIdentifier, accountIdentifier), behavior);
  }
}
