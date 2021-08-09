package software.wings.beans.instance.dashboard.service;

import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class ServiceInstanceDashboard {
  private EntitySummary serviceSummary;
  private List<CurrentActiveInstances> currentActiveInstancesList;
  private List<DeploymentHistory> deploymentHistoryList;
}
