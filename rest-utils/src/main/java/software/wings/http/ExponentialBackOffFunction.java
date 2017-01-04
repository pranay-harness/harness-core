package software.wings.http;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
@FunctionalInterface
public interface ExponentialBackOffFunction<T> {
  T execute() throws IOException;
}
