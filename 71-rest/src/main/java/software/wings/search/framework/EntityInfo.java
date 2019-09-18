package software.wings.search.framework;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityInfoKeys")
public class EntityInfo {
  String id;
  String name;
}
