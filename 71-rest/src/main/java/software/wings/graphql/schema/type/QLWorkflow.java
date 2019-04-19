package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

/**
 * The reason I am defining this class as QLWorkflow
 * is because we already have an enum named as WorkflowType.
 * If required will change the name to something more meaningful.
 */
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLWorkflow implements QLObject {
  String id;
  String name;
  String description;
}
