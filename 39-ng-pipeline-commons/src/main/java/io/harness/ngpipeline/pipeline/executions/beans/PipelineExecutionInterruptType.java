package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.interrupts.ExecutionInterruptType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum PipelineExecutionInterruptType {
  /**
   * Abort all state event.
   */
  @JsonProperty("Abort")
  ABORT("Abort execution of all nodes for the current workflow", ExecutionInterruptType.ABORT_ALL, "Abort"),
  /**
   * Pause all state event.
   */
  @JsonProperty("Pause")
  PAUSE("Pause execution of all nodes for the current workflow", ExecutionInterruptType.PAUSE_ALL, "Pause"),
  /**
   * Resume all state event.
   */
  @JsonProperty("Resume")
  RESUME("Resume execution of all paused nodes in the current workflow", ExecutionInterruptType.RESUME_ALL, "Resume");

  private String description;
  private ExecutionInterruptType executionInterruptType;
  private String displayName;

  PipelineExecutionInterruptType(
      String description, ExecutionInterruptType executionInterruptType, String displayName) {
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

  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }
}
