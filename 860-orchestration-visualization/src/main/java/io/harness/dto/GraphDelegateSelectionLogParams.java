/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dto;

import io.harness.delegate.beans.DelegateSelectionLogParams;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GraphDelegateSelectionLogParams {
  String taskId;
  String taskName;
  DelegateSelectionLogParams selectionLogParams;
}
