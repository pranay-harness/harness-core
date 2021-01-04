package software.wings.graphql.schema.type.aggregation.anomaly;

import software.wings.graphql.schema.type.QLObject;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLAnomalyData implements QLObject {
  String id;
  QLEntityInfo entity;
  String comment;
  Double anomalyScore;
  Double expectedAmount;
  Double actualAmount;
  Long time;
  QLAnomalyFeedback userFeedback;
}
