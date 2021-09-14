/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import io.harness.delegate.beans.DelegateTaskEvent;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DelegateTaskEventsResponse {
  List<DelegateTaskEvent> delegateTaskEvents;
  boolean processTaskEventsAsync;
}
