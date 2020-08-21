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

  private final MorphiaIterator<T, T> iterator;

  private final StopWatch watch = new StopWatch();
  private final long slowProcessing;
  private final long dangerouslySlowProcessing;

  public HIterator(MorphiaIterator<T, T> iterator, long slowProcessing, long dangerouslySlowProcessing) {
    watch.start();
    this.iterator = iterator;
    this.slowProcessing = slowProcessing;
    this.dangerouslySlowProcessing = dangerouslySlowProcessing;
  }

  public HIterator(MorphiaIterator<T, T> iterator) {
    this(iterator, SLOW_PROCESSING, DANGEROUSLY_SLOW_PROCESSING);
  }

  @Override
  public void close() {
    iterator.close();

    watch.stop();
    if (watch.getTime() > slowProcessing) {
      try (CollectionLogContext ignore1 = new CollectionLogContext(iterator.getCollection(), OVERRIDE_NESTS);
           ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(watch.getTime(), OVERRIDE_NESTS)) {
        if (watch.getTime() > dangerouslySlowProcessing) {
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
