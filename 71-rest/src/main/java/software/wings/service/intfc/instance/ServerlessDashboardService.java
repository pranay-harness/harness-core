package software.wings.service.intfc.instance;

import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.InstanceSummaryStatsByService;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public interface ServerlessDashboardService {
  InstanceSummaryStats getAppInstanceSummaryStats(
      @NotEmpty String accountId, List<String> appIds, List<String> groupByEntityTypes, long timestamp);

  PageResponse<InstanceSummaryStatsByService> getAppInstanceSummaryStatsByService(
      String accountId, List<String> appIds, long timestamp, int offset, int limit);

  List<InstanceStatsByEnvironment> getServiceInstances(String accountId, String serviceId, long timestamp);
  ServerlessInstance getInstanceDetails(String instanceId);
  @Nonnull List<ServerlessInstance> getAppInstancesForAccount(@NotEmpty String accountId, long timestamp);
  Set<String> getDeletedAppIds(String accountId, long fromTimestamp, long toTimestamp);
}
