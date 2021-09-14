/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

public interface PipelineViewObject {
  default String toViewJson() {
    return RecastOrchestrationUtils.toJson(this);
  }
}
