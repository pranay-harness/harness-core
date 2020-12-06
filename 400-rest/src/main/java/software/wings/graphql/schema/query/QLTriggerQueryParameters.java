package software.wings.graphql.schema.query;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "QLTriggerQueryParametersKeys")
public class QLTriggerQueryParameters {
  private String triggerId;
  private String triggerName;
  private String applicationId;
}
