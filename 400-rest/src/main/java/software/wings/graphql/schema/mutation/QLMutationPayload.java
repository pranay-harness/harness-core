package software.wings.graphql.schema.mutation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

@TargetModule(Module._380_CG_GRAPHQL)
public interface QLMutationPayload extends ClientMutationIdAccess, QLObject {}
