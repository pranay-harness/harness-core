package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApplicationKeys")
public class QLApplication implements QLObject {
  String id;
  String name;
  String description;
  ZonedDateTime createdAt;
  QLUser createdBy;
}
