package software.wings.service.impl.analysis;

import java.util.Map;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 9/24/17.
 */
@Data
public class TimeSeriesMLTxnSummary {
  private String txn_name;
  private String txn_tag;
  private String group_name;
  private boolean is_key_transaction;
  private Map<String, TimeSeriesMLMetricSummary> metrics;
}
