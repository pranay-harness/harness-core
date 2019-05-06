package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class WorkflowCategoryStepsMeta {
  private String id;
  private String name;
  private List<String> stepIds;
}
