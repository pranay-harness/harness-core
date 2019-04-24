package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class QLApplicationConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLApplication> nodes;
}
