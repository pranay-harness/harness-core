package software.wings.metrics;

/**
 * Possible types of metrics that can be tracked.
 * Created by mike@ on 4/7/17.
 */
public enum MetricType { /**
                          * Metrics used for timing a process.
                          */
  TIME_MS, /**
            * Metrics used for timing a process (DEPRECATED).
            */
  TIME, /**
         * Metrics that count something.
         */
  COUNT, /**
          * Metrics that count something as a rate (count per time)
          */
  RATE, /**
         * Metrics that show the percentage of events that matched.
         */
  PERCENTAGE, /**
               * Metrics that are either true or false.
               */
  BOOLEAN;
}
