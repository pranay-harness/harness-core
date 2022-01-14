package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@Getter
@Setter
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class ParallelInfo {
  private int groupIndex;
}
