/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
