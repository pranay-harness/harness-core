package software.wings.beans.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class ServiceInstanceStatistics extends WingsStatistics {
  private Map<EnvironmentType, List<TopConsumer>> statsMap = new HashMap<>();

  public ServiceInstanceStatistics() {
    super(StatisticsType.SERVICE_INSTANCE_STATISTICS);
  }
}
