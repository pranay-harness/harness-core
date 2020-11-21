package software.wings.service.impl.analysis;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 10/17/17.
 */
@Data
@Builder
public class TimeSeriesMLTxnScores {
  private String transactionName;
  private Map<String, TimeSeriesMLMetricScores> scoresMap;
}
