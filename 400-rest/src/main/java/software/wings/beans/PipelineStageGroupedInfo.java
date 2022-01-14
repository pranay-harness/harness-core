package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class PipelineStageGroupedInfo {
  String name;
  @Singular List<String> pipelineStageElementNames;
  int parallelIndex;
}
