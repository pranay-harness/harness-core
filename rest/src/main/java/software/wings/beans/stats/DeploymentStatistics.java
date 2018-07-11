package software.wings.beans.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class DeploymentStatistics extends WingsStatistics {
  private Map<EnvironmentType, AggregatedDayStats> statsMap = new HashMap<>();

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class AggregatedDayStats {
    private int totalCount;
    private int failedCount;
    private int instancesCount;
    private List<DayStat> daysStats;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DayStat {
      private int totalCount;
      private int failedCount;
      private int instancesCount;
      private Long date;
    }
  }
}
