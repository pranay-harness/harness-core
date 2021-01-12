package software.wings.beans.stats;

import software.wings.beans.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ServiceInstanceStatistics extends WingsStatistics {
  private Map<EnvironmentType, List<TopConsumer>> statsMap = new HashMap<>();

  public ServiceInstanceStatistics() {
    super(StatisticsType.SERVICE_INSTANCE_STATISTICS);
  }
}
