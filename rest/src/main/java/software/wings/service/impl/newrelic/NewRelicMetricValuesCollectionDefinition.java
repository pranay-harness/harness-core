package software.wings.service.impl.newrelic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 8/30/17.
 */
public class NewRelicMetricValuesCollectionDefinition {
  public static final List<String> apdexValues = new ArrayList<>();
  static {
    apdexValues.add("score");
    apdexValues.add("count");
    apdexValues.add("value");
    apdexValues.add("threshold");
    apdexValues.add("threshold_min");
  }

  public static final List<String> webTransactionsValues = new ArrayList<>();
  static {
    webTransactionsValues.add("average_call_time");
    webTransactionsValues.add("average_response_time");
    webTransactionsValues.add("requests_per_minute");
    webTransactionsValues.add("call_count");
    webTransactionsValues.add("min_call_time");
    webTransactionsValues.add("max_call_time");
    webTransactionsValues.add("total_call_time");
    webTransactionsValues.add("throughput");
    webTransactionsValues.add("standard_deviation");
  }

  public static final List<String> allErrors = new ArrayList<>();
  static {
    allErrors.add("errors_per_minute");
    allErrors.add("error_count");
  }
}
