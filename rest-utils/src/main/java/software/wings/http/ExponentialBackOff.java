package software.wings.http;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
public final class ExponentialBackOff {
  private static final int[] FIBONACCI = new int[] {1, 1, 2, 3, 5, 8, 13};

  private ExponentialBackOff() {}

  public static <T, E extends Exception> T execute(ExponentialBackOffFunction<T> fn) throws IOException {
    for (int attempt = 0; attempt < FIBONACCI.length; attempt++) {
      try {
        return fn.execute();
      } catch (IOException e) {
        handleFailure(attempt, e);
      }
    }
    throw new RuntimeException("Failed to communicate.");
  }

  public static <T> T executeForEver(ExponentialBackOffFunction<T> fn) throws IOException {
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
    if (e.getCause() != null) {
      throw e;
    }
    doWait(attempt);
  }

  private static void doWait(int attempt) {
    try {
      Thread.sleep(FIBONACCI[attempt] * 1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
