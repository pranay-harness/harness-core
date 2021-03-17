package software.wings.graphql.provider;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public interface QueryLanguageProvider<T> {
  T getPrivateGraphQL();
  T getPublicGraphQL();
}
