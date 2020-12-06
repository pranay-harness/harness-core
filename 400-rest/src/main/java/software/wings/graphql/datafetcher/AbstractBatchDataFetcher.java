package software.wings.graphql.datafetcher;

import java.util.concurrent.CompletionStage;
import org.dataloader.DataLoader;

public abstract class AbstractBatchDataFetcher<T, P, K> extends AbstractObjectDataFetcher<T, P> {
  protected abstract CompletionStage<T> load(P parameters, DataLoader<K, T> dataLoader);

  @Override
  protected final CompletionStage<T> fetchWithBatching(P parameters, DataLoader dataLoader) {
    return load(parameters, dataLoader);
  }

  @Override
  protected final T fetch(P parameters, String accountId) {
    return null;
  }
}
