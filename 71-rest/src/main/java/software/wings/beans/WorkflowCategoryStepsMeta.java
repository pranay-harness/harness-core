package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class WorkflowCategoryStepsMeta {
  private String id;
  private String name;
  private List<String> stepIds;
}
