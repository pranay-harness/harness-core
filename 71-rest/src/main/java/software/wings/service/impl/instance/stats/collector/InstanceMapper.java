package software.wings.service.impl.instance.stats.collector;

import lombok.AllArgsConstructor;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.beans.infrastructure.instance.stats.Mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
class InstanceMapper implements Mapper<Collection<Instance>, InstanceStatsSnapshot> {
  private Instant instant;
  private String accountId;

  @Override
  public InstanceStatsSnapshot map(Collection<Instance> instances) {
    Collection<AggregateCount> appCounts = aggregateByApp(instances);
    List<AggregateCount> aggregateCounts = new ArrayList<>(appCounts);

    return new InstanceStatsSnapshot(instant, accountId, aggregateCounts);
  }

  private Collection<AggregateCount> aggregateByApp(Collection<Instance> instances) {
    // key = appId
    Map<String, AggregateCount> appCounts = new HashMap<>();

    for (Instance instance : instances) {
      appCounts.putIfAbsent(instance.getAppId(),
          new AggregateCount(EntityType.APPLICATION, instance.getAppName(), instance.getAppId(), 0));
      AggregateCount appCount = appCounts.get(instance.getAppId());
      appCount.incrementCount(1);
    }

    return appCounts.values();
  }

  private Collection<AggregateCount> aggregateByService(Collection<Instance> instances) {
    // key = serviceId
    Map<String, AggregateCount> serviceCounts = new HashMap<>();

    for (Instance instance : instances) {
      serviceCounts.putIfAbsent(instance.getServiceId(),
          new AggregateCount(EntityType.SERVICE, instance.getServiceName(), instance.getServiceId(), 0));
      AggregateCount serviceCount = serviceCounts.get(instance.getServiceId());
      serviceCount.incrementCount(1);
    }

    return serviceCounts.values();
  }
}
