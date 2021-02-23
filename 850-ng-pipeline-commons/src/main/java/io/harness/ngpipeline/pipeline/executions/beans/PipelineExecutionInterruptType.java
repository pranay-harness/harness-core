package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.pms.contracts.interrupts.InterruptType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum PipelineExecutionInterruptType {
  /**
   * Abort all state event.
   */
  @JsonProperty("Abort")
  ABORT("Abort execution of all nodes for the current workflow", InterruptType.ABORT_ALL, "Abort"),
  /**
   * Pause all state event.
   */
  @JsonProperty("Pause")
  PAUSE("Pause execution of all nodes for the current workflow", InterruptType.PAUSE_ALL, "Pause"),
  /**
   * Resume all state event.
   */
  @JsonProperty("Resume")
  RESUME("Resume execution of all paused nodes in the current workflow", InterruptType.RESUME_ALL, "Resume");

  private String description;
  private InterruptType executionInterruptType;
  private String displayName;

  PipelineExecutionInterruptType(String description, InterruptType executionInterruptType, String displayName) {
    this.description = description;
    this.executionInterruptType = executionInterruptType;
    this.displayName = displayName;
  }

  @JsonCreator
  public static PipelineExecutionInterruptType getPipelineExecutionInterrupt(@JsonProperty("type") String displayName) {
    for (PipelineExecutionInterruptType pipelineExecutionInterruptType : PipelineExecutionInterruptType.values()) {
      if (pipelineExecutionInterruptType.displayName.equalsIgnoreCase(displayName)) {
        return pipelineExecutionInterruptType;
      }
    }
    throw new IllegalArgumentException(String.format("Invalid value:%s, the expected values are: %s", displayName,
        Arrays.toString(PipelineExecutionInterruptType.values())));
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  public InterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }
}
