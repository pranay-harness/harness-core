package io.harness.service.stats.usagemetrics.eventpublisher;

import static java.util.stream.Collectors.groupingBy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dtos.InstanceDTO;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.instancestatstimeseriesevent.DataPoint;
import io.harness.eventsframework.schemas.instancestatstimeseriesevent.TimeseriesBatchEventInfo;
import io.harness.service.stats.Constants;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class UsageMetricsEventPublisherImpl implements UsageMetricsEventPublisher {
  private Producer eventProducer;

  public void publishInstanceStatsTimeSeries(String accountId, long timestamp, List<InstanceDTO> instances) {
    if (EmptyPredicate.isEmpty(instances)) {
      return;
    }

    List<DataPoint> dataPointList = new ArrayList<>();
    // key - infraMappingId, value - Set<InstanceDTO>
    Map<String, List<InstanceDTO>> infraMappingInstancesMap =
        instances.stream().collect(groupingBy(InstanceDTO::getInfrastructureMappingId));

    infraMappingInstancesMap.values().forEach(instanceList -> {
      if (EmptyPredicate.isEmpty(instanceList)) {
        return;
      }

      int size = instanceList.size();
      InstanceDTO instance = instanceList.get(0);
      Map<String, String> data = new HashMap<>();
      data.put(Constants.ACCOUNT_ID.getKey(), instance.getAccountIdentifier());
      data.put(Constants.ORG_ID.getKey(), instance.getOrgIdentifier());
      data.put(Constants.PROJECT_ID.getKey(), instance.getProjectIdentifier());
      data.put(Constants.SERVICE_ID.getKey(), instance.getServiceId());
      data.put(Constants.ENV_ID.getKey(), instance.getEnvId());
      data.put(Constants.INFRAMAPPING_ID.getKey(), instance.getInfrastructureMappingId());
      data.put(Constants.CLOUDPROVIDER_ID.getKey(), instance.getConnectorRef());
      data.put(Constants.INSTANCE_TYPE.getKey(), instance.getInstanceType().name());
      data.put(Constants.ARTIFACT_ID.getKey(), instance.getPrimaryArtifact().getArtifactId());
      data.put(Constants.INSTANCECOUNT.getKey(), String.valueOf(size));

      dataPointList.add(DataPoint.newBuilder().putAllData(data).build());
    });

    TimeseriesBatchEventInfo eventInfo = TimeseriesBatchEventInfo.newBuilder()
                                             .setAccountId(accountId)
                                             .setTimestamp(timestamp)
                                             .addAllDataPointList(dataPointList)
                                             .build();

    try {
      // TODO check if more metadata needed to be added
      eventProducer.send(Message.newBuilder()
                             .putAllMetadata(ImmutableMap.of("accountId", accountId))
                             .setData(eventInfo.toByteString())
                             .build());
    } catch (Exception ex) {
      // TODO handle exception gracefully
    }
  }
}
