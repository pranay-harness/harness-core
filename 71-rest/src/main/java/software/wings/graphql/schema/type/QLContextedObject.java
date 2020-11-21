package software.wings.graphql.schema.type;

import java.util.Map;

public interface QLContextedObject {
  Map<String, Object> getContext();
}
