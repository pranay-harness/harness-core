package software.wings.graphql.datafetcher;

import graphql.schema.DataFetchingEnvironment;
import org.dataloader.DataLoader;

import java.util.concurrent.CompletionStage;

public abstract class AbstractBatchDataFetcher<T, P, K> extends AbstractDataFetcher<T, P> {
  protected abstract CompletionStage<T> load(P parameters, DataLoader<K, T> dataLoader);

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) {
    return load(getParameters(dataFetchingEnvironment),
        dataFetchingEnvironment.getDataLoader(getDataFetcherName(dataFetchingEnvironment.getParentType().getName())));
  }

  @Override
  protected final T fetch(P parameters) {
    return null;
  }
}