/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.core.fullsync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;

@OwnedBy(HarnessTeam.DX)
public interface FullSyncAccumulatorService {
  void triggerFullSync(EntityScopeInfo entityScopeInfo, String messageId);
}
