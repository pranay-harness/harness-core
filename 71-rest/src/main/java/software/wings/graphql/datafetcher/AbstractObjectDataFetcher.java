package software.wings.graphql.datafetcher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractObjectDataFetcher<T, P> extends PlainObjectBaseDataFetcher<T, P> {
  protected abstract T fetch(P parameters, String accountId);

  @Override
  protected Object fetchPlainObject(P parameters, String accountId) {
    return fetch(parameters, accountId);
  }
}
