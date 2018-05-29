package software.wings.service.intfc;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

public interface StatisticsService {
  WingsStatistics getTopConsumers(@NotNull String accountId, List<String> appIds);

  WingsStatistics getTopConsumerServices(@NotNull String accountId, List<String> appIds);

  Map<String, AppKeyStatistics> getApplicationKeyStats(List<String> appIds, int numOfDays);

  AppKeyStatistics getSingleApplicationKeyStats(String appId, int numOfDays);

  UserStatistics getUserStats(@NotNull String accountId, List<String> appIds);

  DeploymentStatistics getDeploymentStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);

  NotificationCount getNotificationCount(@NotNull String accountId, List<String> appIds, int minutesFromNow);

  ServiceInstanceStatistics getServiceInstanceStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);

  Map<String, AppKeyStatistics> calculateStringAppKeyStatisticsMap(
      List<String> appIds, Stream<WorkflowExecution> stream);
}
