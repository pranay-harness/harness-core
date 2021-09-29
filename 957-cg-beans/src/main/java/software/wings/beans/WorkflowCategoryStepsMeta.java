package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder(toBuilder = true)
public class WorkflowCategoryStepsMeta {
  private String id;
  private String name;
  private List<String> stepIds;
}
