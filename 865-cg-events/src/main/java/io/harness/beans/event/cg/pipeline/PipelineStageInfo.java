package io.harness.beans.event.cg.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageInfo {
  private String id;
  private String name;
  private String stageType;
  private String status;
  private Long startTime;
  private Long endTime;
}
