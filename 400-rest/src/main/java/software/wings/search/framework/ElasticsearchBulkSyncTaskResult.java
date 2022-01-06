/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
