package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvironmentKeys")
public class QLEnvironment implements QLObject {
  private String id;
  private String name;
  private String description;
  private QLEnvironmentType type;
  private ZonedDateTime createdAt;
  private QLUser createdBy;
}
