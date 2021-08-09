package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
final class ArtifactInfo {
  private String id;
  private String name;
  private String buildNo;
  private String streamId;
  private String streamName;
  private long deployedAt;
  private String sourceName;
  private String lastWorkflowExecutionId;
}
