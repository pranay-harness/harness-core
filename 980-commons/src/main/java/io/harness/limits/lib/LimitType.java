/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum LimitType {
  // specifies a static limit. Example: "100 applications allowed per account"
  // A limit of 0 would mean that a particular feature if forbidden, for cases like: "Free customer can not create a
  // pipeline"
  STATIC,

  // rate limits like "10 deployments per minute allowed"
  RATE_LIMIT,
}
