package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.infrastructure.instance.InstanceType;

@Value
@Builder
public class QLInstance {
  private String id;
  private InstanceType type;
}
