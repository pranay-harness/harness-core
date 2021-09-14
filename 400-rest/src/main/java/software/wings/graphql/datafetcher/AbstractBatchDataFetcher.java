/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.concurrent.CompletionStage;
import org.dataloader.DataLoader;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
