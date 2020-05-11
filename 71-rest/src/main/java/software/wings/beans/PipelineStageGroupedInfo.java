package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
public class PipelineStageGroupedInfo {
  String name;
  @Singular List<String> pipelineStageElementNames;
  int parallelIndex;
}
