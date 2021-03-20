package software.wings.graphql.provider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QueryLanguageProvider<T> {
  T getPrivateGraphQL();
  T getPublicGraphQL();
}
