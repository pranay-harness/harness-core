package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._380_CG_GRAPHQL)
public abstract class AbstractObjectDataFetcher<T, P> extends PlainObjectBaseDataFetcher<T, P> {
  protected abstract T fetch(P parameters, String accountId);

  @Override
  protected Object fetchPlainObject(P parameters, String accountId) {
    return fetch(parameters, accountId);
  }
}
