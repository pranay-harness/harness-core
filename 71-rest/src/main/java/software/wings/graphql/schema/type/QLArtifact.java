package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class QLArtifact implements QLObject, BaseInfo {
  String id;
  String name;
  String sourceName;
  String displayName;
  String buildNo;
  String workflowExecutionName;
  String pipelineExecutionName;
  String lastDeployedBy;
  long lastDeployedAt;
  String debugInfo;
  String appId;
}
