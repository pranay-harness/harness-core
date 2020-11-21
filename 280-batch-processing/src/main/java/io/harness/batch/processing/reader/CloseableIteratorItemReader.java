package io.harness.batch.processing.reader;

import java.util.Iterator;
import org.springframework.batch.item.support.IteratorItemReader;

public class CloseableIteratorItemReader<T> extends IteratorItemReader<T> implements AutoCloseable {
  private AutoCloseable autoCloseable;

  public CloseableIteratorItemReader(Iterator<T> iterator) {
    super(iterator);
    if (iterator instanceof AutoCloseable) {
      this.autoCloseable = (AutoCloseable) iterator;
    }
  }

  @Override
  public void close() throws Exception {
    if (autoCloseable != null) {
      autoCloseable.close();
    }
  }
}
