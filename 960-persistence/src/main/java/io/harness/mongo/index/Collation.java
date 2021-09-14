/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.index;

import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Collation {
  CollationLocale locale;
  CollationStrength strength;
}
