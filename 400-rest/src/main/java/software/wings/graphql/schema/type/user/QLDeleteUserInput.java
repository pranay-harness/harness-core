package software.wings.graphql.schema.type.user;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDeleteUserInput implements QLMutationInput {
  private String clientMutationId;
  private String id;
}
