package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkflowStepMeta {
  private String name;
  private boolean featured;
  private boolean available;
}
