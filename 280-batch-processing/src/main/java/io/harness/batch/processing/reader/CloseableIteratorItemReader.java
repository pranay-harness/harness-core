/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
