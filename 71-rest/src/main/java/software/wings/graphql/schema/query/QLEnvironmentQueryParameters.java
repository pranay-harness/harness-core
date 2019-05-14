package software.wings.graphql.schema.query;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "QLEnvironmentQueryParametersKeys")
public class QLEnvironmentQueryParameters {
  private String environmentId;
}
