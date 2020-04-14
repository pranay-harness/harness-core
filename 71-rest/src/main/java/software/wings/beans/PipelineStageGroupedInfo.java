package software.wings.beans;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PipelineStageGroupedInfo {
  String name;
  @Singular List<String> pipelineStageElementNames;
  int parallelIndex;
}
