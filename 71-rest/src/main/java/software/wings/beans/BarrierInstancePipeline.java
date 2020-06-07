package software.wings.beans;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "BarrierInstancePipelineKeys")
public class BarrierInstancePipeline {
  private String executionId;
  private int parallelIndex;
  List<BarrierInstanceWorkflow> workflows;
}
