package software.wings.service.intfc.dashboardStats;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.stats.dashboard.InstanceDetails;
import software.wings.beans.stats.dashboard.InstanceStatsByService;
import software.wings.beans.stats.dashboard.InstanceSummaryStats;
import software.wings.beans.stats.dashboard.service.ServiceInstanceDashboard;

import java.util.List;

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
   * @return instance summary statistics
   */
  InstanceSummaryStats getAppInstanceSummaryStats(List<String> appIds, List<String> groupByEntityTypes);

  /**
   * Gets the total instance summary stats for the given service.
   * The results are grouped by the given entity types.
   * @param serviceId service id
   * @param groupByEntityTypes the entity types user wants to group by
   * @return instance summary statistics
   */
  InstanceSummaryStats getServiceInstanceSummaryStats(@NotEmpty String serviceId, List<String> groupByEntityTypes);

  /**
   * Gets the total instance stats for the given apps.
   * @param appIds application ids
   * @return instance summary statistics
   */
  List<InstanceStatsByService> getAppInstanceStats(List<String> appIds);

  /**
   * Gets the detailed information about the instances provisioned, deployments and pipelines for the given service.
   * @param serviceId service id
   * @return service dashboard with cloud instance info
   */
  ServiceInstanceDashboard getServiceInstanceDashboard(@NotEmpty String serviceId);

  /**
   * Gets the instance detailed information including the metadata
   * @param instanceId instance id
   * @return instance details with the metadata
   */
  InstanceDetails getInstanceDetails(String instanceId);
}
