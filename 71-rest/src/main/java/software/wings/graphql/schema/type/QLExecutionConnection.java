package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutionConnectionKeys")
public class QLExecutionConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLExecution> nodes;
}
