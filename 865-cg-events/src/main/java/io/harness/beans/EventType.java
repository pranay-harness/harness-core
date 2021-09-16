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
  WORKFLOW_START("workflow_start"),
  WORKFLOW_END("workflow_end");

  String eventValue;

  EventType(String eventType) {
    this.eventValue = eventType;
  }

  @JsonValue
  public String getEventValue() {
    return eventValue;
  }

  public static List<String> getPipelineEvents() {
    return Lists.newArrayList(PIPELINE_START, PIPELINE_END)
        .stream()
        .map(EventType::getEventValue)
        .collect(Collectors.toList());
  }

  public static List<String> getWorkflowEvents() {
    return Lists.newArrayList(WORKFLOW_START, WORKFLOW_END)
        .stream()
        .map(EventType::getEventValue)
        .collect(Collectors.toList());
  }
}
