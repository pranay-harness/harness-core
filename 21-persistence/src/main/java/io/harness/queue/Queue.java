package io.harness.queue;

import java.time.Duration;
import java.util.Date;

/**
 * The Interface Queue.
 *
 * @param <T> the generic type
 */
public interface Queue<T> {
  /**
   * Get a non running message from queue.
   *
   * @param wait duration in milliseconds to keep polling before returning null
   * @param poll duration in milliseconds between poll attempts
   * @return message or null
   */
  T get(Duration wait, Duration poll);

  /**
   * Update the refresh duration of a message while still processing.
   *
   * @param message message received from get(). Should not be null.
   */
  void updateResetDuration(T message);

  enum Filter { ALL, RUNNING, NOT_RUNNING }

  /**
   * Count in queue.
   *
   * @param filter what to return
   * @return count long
   */
  long count(Filter filter);

  /**
   * Acknowledge a message was processed and remove from queue.
   *
   * @param message message received from get(). Should not be null.
   */
  void ack(T message);

  /**
   * Requeue message with id.
   *
   * @param id the message id
   * @param retries the retries left
   */
  void requeue(String id, int retries);

  /**
   * Requeue message with id.
   *
   * @param id the message id
   * @param retries the retries left
   * @param earliestGet earliest instant that a call to get() can return message. Should not be null
   */
  void requeue(String id, int retries, Date earliestGet);

  /**
   * Send message to queue with earliestGet as Now and 0.0 priority
   *
   * @param payload payload. Should not be null
   */
  void send(T payload);

  /**
   * reset duration in milliseconds.
   *
   * @return reset duration in milliseconds
   */
  long resetDurationMillis();

  /**
   * Returns the name of the queue.
   *
   * @return the string
   */
  String name();
}
