package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QLArtifact implements QLObject {
  String id;
  String name;
  String sourceName;
  String displayName;
  String buildNo;
  String workflowExecutionName;
  String pipelineExecutionName;
  String lastDeployedBy;
  long lastDeployedAt;
  String appId;
}
