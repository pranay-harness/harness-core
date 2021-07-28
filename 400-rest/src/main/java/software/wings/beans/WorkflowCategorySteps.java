package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@TargetModule(HarnessModule._959_CG_BEANS)
@OwnedBy(CDC)
@Value
@Builder
public class WorkflowCategorySteps {
  private Map<String, WorkflowStepMeta> steps;
  private List<WorkflowCategoryStepsMeta> categories;
}
