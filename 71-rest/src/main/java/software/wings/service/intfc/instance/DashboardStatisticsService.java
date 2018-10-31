package software.wings.service.intfc.instance;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Serves all the service and infrastructure dashboard related statistics
 * @author rktummala on 08/13/17
 */
public interface DashboardStatisticsService {
  /**
   * Gets the total instance summary stats for the given apps.
   * The results are grouped by the given entity types.
   * @param appIds application ids
   * @param groupByEntityTypes the entity types user wants to group by
   * @param timestamp point in time
   * @return instance summary statistics
   */
  InstanceSummaryStats getAppInstanceSummaryStats(List<String> appIds, List<String> groupByEntityTypes, long timestamp);

  /**
   * Gets the total instance summary stats for the given service.
   * The results are grouped by the given entity types.
   * @param serviceId service id
   * @param groupByEntityTypes the entity types user wants to group by
   * @param timestamp point in time
   * @return instance summary statistics
   */
  InstanceSummaryStats getServiceInstanceSummaryStats(
      @NotEmpty String serviceId, List<String> groupByEntityTypes, long timestamp);

  /**
   * Gets the total instance stats for the given apps.
   * @param appIds application ids
   * @param timestamp point in time
   * @return instance summary statistics
   */
  List<InstanceStatsByService> getAppInstanceStatsByService(List<String> appIds, long timestamp);

  /**
   * Gets the instances for the given account. This api is used by the stats cron job.
   * @param accountId account id
   * @param timestamp point in time
   * @return set of instances
   */
  @Nonnull Set<Instance> getAppInstancesForAccount(String accountId, long timestamp);

  /**
   * Gets the detailed information about the instances provisioned, deployments and pipelines for the given service.
   * @param appId app id
   * @param serviceId service id
   * @return service dashboard with cloud instance info
   */
  ServiceInstanceDashboard getServiceInstanceDashboard(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * Gets the instance detailed information including the metadata
   * @param instanceId instance id
   * @return instance info with the metadata
   */
  Instance getInstanceDetails(String instanceId);

  Set<String> getDeletedAppIds(String accountId, long timestamp);
}
