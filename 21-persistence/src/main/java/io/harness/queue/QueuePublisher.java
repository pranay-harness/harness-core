package io.harness.queue;

/**
 * The Interface Queue.
 */
public interface QueuePublisher<T extends Queuable> extends Queue { void send(T payload); }
