package software.wings.graphql.schema.type.aggregation.anomaly;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(Module._380_CG_GRAPHQL)
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
