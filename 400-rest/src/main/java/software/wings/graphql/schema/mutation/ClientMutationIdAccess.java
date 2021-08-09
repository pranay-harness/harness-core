package software.wings.graphql.schema.mutation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface ClientMutationIdAccess {
  String getClientMutationId();
}
