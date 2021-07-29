package io.harness.beans.event;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;

@OwnedBy(CDC)
public abstract class EventPayloadData {
  @JsonIgnore
  public String getPipelineId() {
    return null;
  }

  @JsonIgnore
  public String getWorkflowId() {
    return null;
  }
}
