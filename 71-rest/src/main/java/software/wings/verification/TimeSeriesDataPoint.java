package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 13/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {
  private long timestamp;
  private float value;
}
