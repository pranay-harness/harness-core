package software.wings.metrics;

/**
 * Possible types of metrics that can be tracked.
 * Created by mike@ on 4/7/17.
 */
public enum MetricType {
  /**
   * Metrics that represents any observation
   */
  VALUE,

  /**
   * Metrics that measure time
   */
  RESP_TIME,

  /**
   * Metrics that count invocations
   */
  THROUGHPUT,

  /**
   * Metrics that count error
   */
  ERROR,

  /**
   * Metrics that count something
   */
  COUNT
}
