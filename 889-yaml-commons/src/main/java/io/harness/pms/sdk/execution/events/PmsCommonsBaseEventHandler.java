/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.execution.events;

import java.util.Map;

public interface PmsCommonsBaseEventHandler<T> {
  void handleEvent(T event, Map<String, String> metadataMap, long timestamp);
}
