package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by rsingh on 9/5/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewRelicWebTransactions {
  private double average_call_time;
  private double average_response_time;
  private long requests_per_minute;
  private long call_count;
  private double min_call_time;
  private double max_call_time;
  private long total_call_time;
  private double throughput;
  private double standard_deviation;
}
