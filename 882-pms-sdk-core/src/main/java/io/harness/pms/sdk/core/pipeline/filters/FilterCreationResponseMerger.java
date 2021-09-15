/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.filter.creation.FilterCreationResponse;

public interface FilterCreationResponseMerger {
  void mergeFilterCreationResponse(FilterCreationResponse finalResponse, FilterCreationResponse current);
}
