package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentInfo implements DebugInfo {
  String id;
  String name;
  String description;
  String environmentType;
  String debugInfo;
}
