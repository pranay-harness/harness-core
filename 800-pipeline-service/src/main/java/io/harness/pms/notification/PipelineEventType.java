package io.harness.pms.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PipelineEventType {
  @JsonProperty("AllEvents") ALL_EVENTS("Pipeline"),
  @JsonProperty("PipelineSuccess") PIPELINE_SUCCESS("Pipeline"),
  @JsonProperty("PipelineFailed") PIPELINE_FAILED("Pipeline"),
  @JsonProperty("PipelinePaused") PIPELINE_PAUSED("Pipeline"),
  @JsonProperty("StageSuccess") STAGE_SUCCESS("Stage"),
  @JsonProperty("StageFailed") STAGE_FAILED("Stage"),
  @JsonProperty("StageStart") STAGE_START("Stage"),
  @JsonProperty("StageStart") STEP_FAILED("Step");

  private String level;

  PipelineEventType(String level) {
    this.level = level;
  }

  public String getLevel() {
    return level;
  }
}
