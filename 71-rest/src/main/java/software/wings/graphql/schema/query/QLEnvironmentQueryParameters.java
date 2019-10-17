package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEnvironmentQueryParametersKeys")
public class QLEnvironmentQueryParameters {
  private String environmentId;
}
