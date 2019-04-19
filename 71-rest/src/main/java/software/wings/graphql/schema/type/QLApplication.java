package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLApplication implements QLObject, BaseInfo {
  String id;
  String name;
  String description;
  String debugInfo;
}
