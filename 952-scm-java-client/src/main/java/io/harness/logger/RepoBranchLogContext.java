/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logger;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.data.structure.NullSafeImmutableMap.NullSafeBuilder;
import io.harness.logging.AutoLogContext;

import java.util.Map;

@OwnedBy(DX)
public class RepoBranchLogContext extends AutoLogContext {
  private static Map<String, String> getContext(String slug, String branchName, String ref) {
    NullSafeBuilder<String, String> nullSafeBuilder = NullSafeImmutableMap.builder();
    nullSafeBuilder.putIfNotNull("branchName", branchName);
    nullSafeBuilder.putIfNotNull("ref", ref);
    nullSafeBuilder.putIfNotNull("slug", slug);
    return nullSafeBuilder.build();
  }

  public RepoBranchLogContext(String slug, String branchName, String ref, OverrideBehavior behavior) {
    super(getContext(slug, branchName, ref), behavior);
  }
}
