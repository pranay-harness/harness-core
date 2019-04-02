package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class WorkflowCategorySteps {
  private Map<String, WorkflowStepMeta> steps;
  private List<WorkflowCategoryStepsMeta> categories;
}
