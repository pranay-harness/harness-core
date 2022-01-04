package io.harness.beans.event.cg.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepInfo {
  private String id;
  private String name;
  private String stepType;
  private String status;
  private Long startTime;
  private Long endTime;
}
