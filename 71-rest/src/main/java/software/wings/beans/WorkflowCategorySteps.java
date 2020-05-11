package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
public class WorkflowCategorySteps {
  private Map<String, WorkflowStepMeta> steps;
  private List<WorkflowCategoryStepsMeta> categories;
}
