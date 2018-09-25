package io.harness.queue;

import java.util.Date;

/**
 * The Interface Queue.
 *
 * @param <T> the generic type
 */
public interface Queue<T> {
  /**
   * Get a non running message from queue with a wait of 3 seconds and poll of 1 second.
   *
   * @return message or null
   */
  T get();

  /**
   * Get a non running message from queue with a poll of 1 second.
   *
   * @param waitDuration duration in milliseconds to keep polling before returning null
   * @return message or null
   */
  T get(int waitDuration);

  /**
   * Get a non running message from queue.
   *
   * @param waitDuration duration in milliseconds to keep polling before returning null
   * @param pollDuration duration in milliseconds between poll attempts
   * @return message or null
   */
  T get(int waitDuration, long pollDuration);

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
   * Ack message and send payload to queue, atomically, with earliestGet as Now and 0.0 priority
   *
   * @param message message to ack received from get(). Should not be null
   * @param payload payload to send. Should not be null
   */
  void ackSend(T message, T payload);

  /**
   * Requeue message with earliestGet as Now and 0.0 priority. Same as ackSend() with the same
   * message.
   *
   * @param message message to requeue received from get(). Should not be null
   */
  void requeue(T message);

  /**
   * Requeue message with 0.0 priority. Same as ackSend() with the same message.
   *
   * @param message     message to requeue received from get(). Should not be null
   * @param earliestGet earliest instant that a call to get() can return message. Should not be null
   */
  void requeue(T message, Date earliestGet);

  /**
   * Requeue message. Same as ackSend() with the same message.
   *
   * @param message     message to requeue received from get(). Should not be null
   * @param earliestGet earliest instant that a call to get() can return message. Should not be null
   * @param priority    priority for order out of get(). 0 is higher priority than 1. Should not be NaN
   */
  void requeue(T message, Date earliestGet, double priority);

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
