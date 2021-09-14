/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
