package software.wings.beans.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class WingsStatistics {
  private StatisticsType type;

  public enum StatisticsType { DEPLOYMENT, SERVICE_INSTANCE_STATISTICS }
}
