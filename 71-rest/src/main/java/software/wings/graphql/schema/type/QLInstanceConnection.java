package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstanceConnection implements QLObject {
  QLPageInfo pageInfo;
  @Singular List<QLInstance> nodes;
}
