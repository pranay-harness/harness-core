/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;

@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class DelegateLogContext extends AutoLogContext {
  public static final String ACCOUNT_ID = "accountId";
  public static final String DELEGATE_ID = "delegateId";

  public DelegateLogContext(String delegateId, OverrideBehavior behavior) {
    super(DELEGATE_ID, delegateId, behavior);
  }

  public DelegateLogContext(String accountId, String delegateId, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .putIfNotNull(ACCOUNT_ID, accountId)
              .putIfNotNull(DELEGATE_ID, delegateId)
              .build(),
        behavior);
  }
}
