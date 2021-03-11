package software.wings.graphql.schema.mutation.tag;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAttachTagInputKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDetachTagInput implements QLMutationInput {
  private String clientMutationId;
  private String entityId;
  private String name;
  private QLEntityType entityType;
}
