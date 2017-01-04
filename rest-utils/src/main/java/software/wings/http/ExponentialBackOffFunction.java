package software.wings.http;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
@FunctionalInterface
public interface ExponentialBackOffFunction<T, E extends Exception> {
  T execute() throws E;
}
