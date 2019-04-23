package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.beans.infrastructure.instance.InstanceType;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstance {
  String id;
  InstanceType type;

  String serviceId;
  String envId;
  String appId;
}
