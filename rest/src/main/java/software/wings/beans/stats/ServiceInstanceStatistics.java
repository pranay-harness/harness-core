package software.wings.beans.stats;

import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 7/30/17.
 */
public class ServiceInstanceStatistics extends WingsStatistics {
  private Map<EnvironmentType, List<TopConsumer>> statsMap = new HashMap<>();

  public ServiceInstanceStatistics() {
    super(StatisticsType.SERVICE_INSTANCE_STATISTICS);
  }
  /**
   * Gets stats map.
   *
   * @return the stats map
   */
  public Map<EnvironmentType, List<TopConsumer>> getStatsMap() {
    return statsMap;
  }

  /**
   * Sets stats map.
   *
   * @param statsMap the stats map
   */
  public void setStatsMap(Map<EnvironmentType, List<TopConsumer>> statsMap) {
    this.statsMap = statsMap;
  }
}
