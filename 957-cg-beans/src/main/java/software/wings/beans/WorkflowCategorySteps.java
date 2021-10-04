package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class WorkflowCategorySteps {
  private Map<String, software.wings.beans.WorkflowStepMeta> steps;
  private List<software.wings.beans.WorkflowCategoryStepsMeta> categories;
}
