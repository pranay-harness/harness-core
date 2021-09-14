/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.reader;

import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

@Slf4j
public abstract class BatchedItemReader<I> implements ItemReader<I> {
  protected static final int DEFAULT_READER_BATCH_SIZE = 500;
  private List<I> items;
  private Integer itemIndex;

  /**
   * @return next batched items to be read.
   */
  @NonNull protected abstract List<I> getMore();

  @Override
  public I read() {
    if (isInitialReadOrAllRead() && !readNextBatch()) {
      // ItemReader expects null if no item left.
      return null;
    }
    return items.get(itemIndex++);
  }

  private boolean isInitialReadOrAllRead() {
    return itemIndex == null || items == null || itemIndex >= items.size();
  }

  private boolean readNextBatch() {
    itemIndex = 0;
    items = getMore();
    return !items.isEmpty();
  }
}
