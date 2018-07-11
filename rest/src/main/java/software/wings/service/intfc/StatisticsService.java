package software.wings.service.intfc;

import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.ServiceInstanceStatistics;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface StatisticsService {
  DeploymentStatistics getDeploymentStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);

  ServiceInstanceStatistics getServiceInstanceStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);
}
