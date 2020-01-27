package io.harness.persistence;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.logging.AutoLogContext;
import io.harness.mongo.CollectionLogContext;
import io.harness.mongo.ProcessTimeLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.mongodb.morphia.query.MorphiaIterator;

import java.util.Iterator;

// This is a simple wrapper around MorphiaIterator to provide AutoCloseable implementation
@Slf4j
public class HIterator<T> implements AutoCloseable, Iterable<T>, Iterator<T> {
  private static final long SLOW_PROCESSING = 1000;
  private static final long DANGEROUSLY_SLOW_PROCESSING = 5000;

  private MorphiaIterator<T, T> iterator;

  private StopWatch watch = new StopWatch();

  public HIterator(MorphiaIterator<T, T> iterator) {
    watch.start();
    this.iterator = iterator;
  }

  @Override
  public void close() {
    iterator.close();

    watch.stop();
    if (watch.getTime() > SLOW_PROCESSING) {
      try (CollectionLogContext ignore1 = new CollectionLogContext(iterator.getCollection(), OVERRIDE_NESTS);
           ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(watch.getTime(), OVERRIDE_NESTS)) {
        if (watch.getTime() > DANGEROUSLY_SLOW_PROCESSING) {
          logger.error("HIterator is dangerously slow processing the data for query: {}",
              iterator.getCursor().getQuery().toString());
        } else {
          logger.info("Time consuming HIterator processing");
        }
      }
    }
  }

  @Override
  public boolean hasNext() {
    return HPersistence.retry(() -> iterator.hasNext());
  }

  @Override
  public T next() {
    try (AutoLogContext ignore = new CollectionLogContext(iterator.getCollection(), OVERRIDE_ERROR)) {
      return HPersistence.retry(() -> iterator.next());
    }
  }

  @Override
  // This is just wrapper around the morphia iterator, it cannot be reused anyways
  @SuppressWarnings("squid:S4348")
  public Iterator<T> iterator() {
    return this;
  }
}
