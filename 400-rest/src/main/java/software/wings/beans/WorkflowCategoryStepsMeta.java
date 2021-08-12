package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class WorkflowCategoryStepsMeta {
  private String id;
  private String name;
  private List<String> stepIds;
}
