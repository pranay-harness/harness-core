package software.wings.service.impl.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class WorkflowNotificationDetails {
  @Builder.Default private String url = "";
  @Builder.Default private String name = "";
  @Builder.Default private String message = "";
}
