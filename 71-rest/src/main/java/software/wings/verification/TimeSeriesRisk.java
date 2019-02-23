package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesRisk {
  private long startTime;
  private long endTime;
  private int risk;
}
