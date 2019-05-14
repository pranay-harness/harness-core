package software.wings.graphql.schema.query;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "QLExecutionQueryParametersKeys")
public class QLExecutionQueryParameters {
  private String executionId;
}
