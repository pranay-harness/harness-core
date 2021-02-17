package software.wings.graphql.schema.mutation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public interface ClientMutationIdAccess {
  String getClientMutationId();
}
