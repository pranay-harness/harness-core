/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public enum EventType {
  TEST("test"),
  PIPELINE_START("pipeline_start"),
  PIPELINE_END("pipeline_end"),
  PIPELINE_PAUSE("pipeline_pause"),
  PIPELINE_CONTINUE("pipeline_continue"),
  WORKFLOW_START("workflow_start"),
  WORKFLOW_END("workflow_end"),
  WORKFLOW_PAUSE("workflow_pause"),
  WORKFLOW_CONTINUE("workflow_continue");

  String eventValue;

  EventType(String eventType) {
    this.eventValue = eventType;
  }

  @JsonValue
  public String getEventValue() {
    return eventValue;
  }

  public static List<String> getPipelineEvents() {
    return Lists.newArrayList(PIPELINE_START, PIPELINE_END, PIPELINE_PAUSE, PIPELINE_CONTINUE)
        .stream()
        .map(EventType::getEventValue)
        .collect(Collectors.toList());
  }

  public static List<String> getWorkflowEvents() {
    return Lists.newArrayList(WORKFLOW_START, WORKFLOW_END, WORKFLOW_PAUSE, WORKFLOW_CONTINUE)
        .stream()
        .map(EventType::getEventValue)
        .collect(Collectors.toList());
  }
}
