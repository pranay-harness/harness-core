package software.wings.service.impl.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowNotificationDetails {
  @Builder.Default private String url = "";
  @Builder.Default private String name = "";
  @Builder.Default private String message = "";
}
