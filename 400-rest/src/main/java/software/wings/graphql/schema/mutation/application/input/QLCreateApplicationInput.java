package software.wings.graphql.schema.mutation.application.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLCreateApplicationInput implements QLMutationInput {
  String clientMutationId;
  String name;
  String description;
  Boolean isManualTriggerAuthorized;
}
