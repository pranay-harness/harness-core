package software.wings.graphql.schema.type.aggregation.anomaly;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLAnomalyInput implements QLMutationInput {
  String clientMutationId;
  String anomalyId;
  @JsonProperty("comment") String note;
  QLAnomalyFeedback userFeedback;
}
