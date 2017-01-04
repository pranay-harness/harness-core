package software.wings.http;

import static java.util.Arrays.asList;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
public final class ExponentialBackOff {
  private static final int[] FIBONACCI = new int[] {1, 1, 2, 3, 5, 8, 13};
  private static final List<Class<? extends Exception>> EXPECTED_COMMUNICATION_ERRORS =
      asList(ConnectException.class, SocketTimeoutException.class);

  private ExponentialBackOff() {}

  public static <T, E extends Exception> T execute(ExponentialBackOffFunction<T, E> fn) throws E {
    for (int attempt = 0; attempt < FIBONACCI.length; attempt++) {
      try {
        return fn.execute();
      } catch (Exception e) {
        handleFailure(attempt, (E) e);
      }
    }
    throw new RuntimeException("Failed to communicate.");
  }

  public static <T, E extends Exception> T executeForEver(ExponentialBackOffFunction<T, E> fn) throws E {
    for (int attempt = 0; attempt < FIBONACCI.length; attempt++) {
      try {
        return fn.execute();
      } catch (Exception e) {
        handleFailure(attempt, (E) e);
      }
    }
    int attempt = FIBONACCI.length - 1;
    while (true) {
      try {
        return fn.execute();
      } catch (Exception e) {
        handleFailure(attempt, (E) e);
      }
    }
  }

  private static <E extends Exception> void handleFailure(int attempt, E e) throws E {
    if (e.getCause() != null && !EXPECTED_COMMUNICATION_ERRORS.contains(e.getCause().getClass())) {
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
