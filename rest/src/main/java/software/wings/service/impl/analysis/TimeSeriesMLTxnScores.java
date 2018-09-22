package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLTxnScores {
  private String transactionName;
  private Map<String, TimeSeriesMLMetricScores> scoresMap;
}
