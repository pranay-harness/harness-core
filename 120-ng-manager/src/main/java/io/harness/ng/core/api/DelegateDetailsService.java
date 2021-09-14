/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;

@OwnedBy(DEL)
public interface DelegateDetailsService {
  long getDelegateGroupCount(String accountId, @Nullable String orgId, @Nullable String projectId);
}
