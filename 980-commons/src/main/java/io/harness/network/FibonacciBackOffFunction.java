package io.harness.network;

import java.io.IOException;

@FunctionalInterface
public interface FibonacciBackOffFunction<T> {
  T execute() throws IOException;
}
