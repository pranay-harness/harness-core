package software.wings.sm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineSummary {
  private String pipelineId;
  private String pipelineName;
}
