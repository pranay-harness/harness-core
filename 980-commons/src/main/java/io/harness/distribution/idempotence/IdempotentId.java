/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.idempotence;

import lombok.AllArgsConstructor;
import lombok.Value;

/*
 * Extra strong typed idempotent id class.
 */

@Value
@AllArgsConstructor
public class IdempotentId {
  private String value;
}
