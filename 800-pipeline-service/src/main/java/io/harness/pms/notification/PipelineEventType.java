package io.harness.pms.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PipelineEventType {
  @JsonProperty(PipelineEventTypeConstants.ALL_EVENTS) ALL_EVENTS("Pipeline", PipelineEventTypeConstants.ALL_EVENTS),
  @JsonProperty(PipelineEventTypeConstants.PIPELINE_START)
  PIPELINE_START("Pipeline", PipelineEventTypeConstants.PIPELINE_START),
  @JsonProperty(PipelineEventTypeConstants.PIPELINE_SUCCESS)
  PIPELINE_SUCCESS("Pipeline", PipelineEventTypeConstants.PIPELINE_SUCCESS),
  @JsonProperty(PipelineEventTypeConstants.PIPELINE_FAILED)
  PIPELINE_FAILED("Pipeline", PipelineEventTypeConstants.PIPELINE_FAILED),
  @JsonProperty(PipelineEventTypeConstants.PIPELINE_PAUSED)
  PIPELINE_PAUSED("Pipeline", PipelineEventTypeConstants.PIPELINE_PAUSED),
  @JsonProperty(PipelineEventTypeConstants.STAGE_SUCCESS)
  STAGE_SUCCESS("Stage", PipelineEventTypeConstants.STAGE_SUCCESS),
  @JsonProperty(PipelineEventTypeConstants.STAGE_FAILED) STAGE_FAILED("Stage", PipelineEventTypeConstants.STAGE_FAILED),
  @JsonProperty(PipelineEventTypeConstants.STAGE_START) STAGE_START("Stage", PipelineEventTypeConstants.STAGE_START),
  @JsonProperty(PipelineEventTypeConstants.STEP_FAILED) STEP_FAILED("Step", PipelineEventTypeConstants.STEP_FAILED);

  private String level;
  private String displayName;

  PipelineEventType(String level, String displayName) {
    this.level = level;
    this.displayName = displayName;
  }

  public String getLevel() {
    return level;
  }

  public String getDisplayName() {
    return displayName;
  }
}
