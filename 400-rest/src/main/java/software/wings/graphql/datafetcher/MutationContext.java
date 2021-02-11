package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingEnvironment;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class MutationContext {
  private String accountId;
  private DataFetchingEnvironment dataFetchingEnvironment;
}
