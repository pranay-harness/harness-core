package io.harness.batch.processing.reader;

import java.util.List;
import org.springframework.batch.item.ItemReader;

public abstract class BatchedItemReader<I> implements ItemReader<I> {
  protected static final int DEFAULT_READER_BATCH_SIZE = 500;
  protected List<I> items;
  protected int itemIndex;

  /**
   * @return boolean; return true if there is at least one item available to be read, else false
   */
  protected abstract boolean readNextBatch();

  @Override
  public I read() {
    if ((items == null || itemIndex >= items.size()) && !readNextBatch()) {
      // ItemReader expects null if no item left.
      return null;
    }

    return items.get(itemIndex++);
  }
}
