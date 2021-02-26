package software.wings.graphql.schema.mutation.application.input;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationInputKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUpdateApplicationInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private RequestField<String> name;
  private RequestField<String> description;
}
