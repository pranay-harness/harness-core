package software.wings.graphql.datafetcher;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractArrayDataFetcher<T, P> extends PlainObjectBaseDataFetcher<T, P> {
  protected abstract List<T> fetch(P parameters, String accountId);

  // In get() of PlainObjectBaseDataFetcher, we are trying to find the return class
  // Class<T> returnClass = (Class<T>) typeArguments[0];
  // However Java is optimizing and not passing the generic <T> assuming that this is not used.
  // To get around this, we are creating a dummy method so that Java think that this will be used downstream and
  // doesn't do any optimization around that.
  protected abstract T unusedReturnTypePassingDummyMethod();

  @Override
  protected Object fetchPlainObject(P parameters, String accountId) {
    return fetch(parameters, accountId);
  }
}
