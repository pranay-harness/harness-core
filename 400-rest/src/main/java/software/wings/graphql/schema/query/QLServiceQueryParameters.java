package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceQueryParametersKeys")
public class QLServiceQueryParameters {
  private String serviceId;
}
