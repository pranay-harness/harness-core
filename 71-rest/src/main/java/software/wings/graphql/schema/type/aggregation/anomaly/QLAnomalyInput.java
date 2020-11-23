package software.wings.graphql.schema.type.aggregation.anomaly;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLAnomalyInput implements QLMutationInput {
  String clientMutationId;
  String anomalyId;
  String comment;
  QLAnomalyFeedback userFeedback;
}
