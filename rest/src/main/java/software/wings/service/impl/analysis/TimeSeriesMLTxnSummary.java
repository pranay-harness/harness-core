package software.wings.service.impl.analysis;

import lombok.Data;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/24/17.
 */
@Data
public class TimeSeriesMLTxnSummary {
  private String txn_name;
  private String txn_tag;
  private String group_name;
  private Map<String, TimeSeriesMLMetricSummary> metrics;
}
