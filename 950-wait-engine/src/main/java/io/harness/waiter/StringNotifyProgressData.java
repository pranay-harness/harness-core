/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.waiter;

import io.harness.tasks.ProgressData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StringNotifyProgressData implements ProgressData {
  String data;
}
