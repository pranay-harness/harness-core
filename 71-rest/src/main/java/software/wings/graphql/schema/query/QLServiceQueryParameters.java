package software.wings.graphql.schema.query;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "QLServiceQueryParametersKeys")
public class QLServiceQueryParameters {
  private String serviceId;
}
