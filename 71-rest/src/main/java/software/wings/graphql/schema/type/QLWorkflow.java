package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowKeys")
public class QLWorkflow implements QLObject {
  private String id;
  private String name;
  private String description;
}
