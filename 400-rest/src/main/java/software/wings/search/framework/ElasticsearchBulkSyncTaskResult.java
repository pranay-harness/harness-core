/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;

import java.util.Queue;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
class ElasticsearchBulkSyncTaskResult {
  private boolean isSuccessful;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync;
}
