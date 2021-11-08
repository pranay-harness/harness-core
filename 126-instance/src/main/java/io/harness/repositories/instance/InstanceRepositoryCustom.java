package io.harness.repositories.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.Instance;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;

import java.util.List;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.DX)
public interface InstanceRepositoryCustom {
  Instance findAndReplace(Criteria criteria, Instance instance);

  Instance findAndModify(Criteria criteria, Update update);

  List<Instance> getActiveInstancesByAccount(String accountIdentifier, long timestamp);

  List<Instance> getInstancesModifiedInInterval(String accountIdentifier, long startTimestamp, long endTimeStamp);

  List<Instance> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId);

  List<Instance> getActiveInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);

  List<Instance> getActiveInstancesByInstanceInfo(
      String accountIdentifier, String instanceInfoNamespace, String instanceInfoPodName);

  List<Instance> getActiveInstancesByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);

  List<Instance> getActiveInstancesByInfrastructureMappingId(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String infrastructureMappingId, long timestampInMs);

  AggregationResults<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs);

  AggregationResults<InstancesByBuildId> getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String serviceId, String envId, List<String> buildIds,
      long timestampInMs, int limit);

  AggregationResults<CountByServiceIdAndEnvType> getActiveServiceInstanceCountBreakdown(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, List<String> serviceId, long timestampInMs);
}
