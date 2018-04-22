package io.harness.network;

import static io.harness.threading.Morpheus.sleep;

import java.io.IOException;
import java.time.Duration;

public final class FibonacciBackOff {
  private static final int[] FIBONACCI = new int[] {1, 1, 2, 3, 5, 8, 13};

  private FibonacciBackOff() {}

  public static <T, E extends Exception> T execute(FibonacciBackOffFunction<T> fn) throws IOException {
    for (int attempt = 0; attempt < FIBONACCI.length; attempt++) {
      try {
        return fn.execute();
      } catch (IOException e) {
        handleFailure(attempt, e);
      }
    }
    throw new RuntimeException("Failed to communicate.");
  }

  public static <T> T executeForEver(FibonacciBackOffFunction<T> fn) throws IOException {
    for (int attempt = 0; attempt < FIBONACCI.length; attempt++) {
      try {
        return fn.execute();
      } catch (IOException e) {
        handleFailure(attempt, e);
      }
    }
    int attempt = FIBONACCI.length - 1;
    while (true) {
      try {
        return fn.execute();
      } catch (IOException e) {
        handleFailure(attempt, e);
      }
    }
  }

  private static void handleFailure(int attempt, IOException e) throws IOException {
    IOException ex = peel(e);
    if (ex.getCause() != null) {
      throw ex;
    }
    sleep(Duration.ofSeconds(FIBONACCI[attempt]));
  }

  public static IOException peel(IOException t) {
    while (t.getCause() != null && t.getCause() instanceof IOException) {
      t = (IOException) t.getCause();
    }
    return t;
  }
}
