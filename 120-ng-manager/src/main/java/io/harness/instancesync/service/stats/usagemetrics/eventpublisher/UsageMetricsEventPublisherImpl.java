package io.harness.instancesync.service.stats.usagemetrics.eventpublisher;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.groupingBy;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.timeseriesevent.DataPoint;
import io.harness.eventsframework.schemas.timeseriesevent.TimeseriesBatchEventInfo;
import io.harness.instancesync.dto.Instance;
import io.harness.instancesync.service.stats.Constants;

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
public class UsageMetricsEventPublisherImpl implements UsageMetricsEventPublisher {
  private Producer eventProducer;

  public void publishInstanceStatsTimeSeries(String accountId, long timestamp, List<Instance> instances) {
    if (isEmpty(instances)) {
      return;
    }

    List<DataPoint> dataPointList = new ArrayList<>();
    // key - infraMappingId, value - Set<Instance>
    Map<String, List<Instance>> infraMappingInstancesMap =
        instances.stream().collect(groupingBy(Instance::getInfrastructureMappingId));

    infraMappingInstancesMap.values().forEach(instanceList -> {
      if (isEmpty(instanceList)) {
        return;
      }

      int size = instanceList.size();
      Instance instance = instanceList.get(0);
      Map<String, String> data = new HashMap<>();
      data.put(Constants.ACCOUNT_ID.getKey(), instance.getAccountId());
      // TODO check replacement of Appid

      data.put(Constants.SERVICE_ID.getKey(), instance.getServiceId());
      data.put(Constants.ENV_ID.getKey(), instance.getEnvId());
      data.put(Constants.INFRAMAPPING_ID.getKey(), instance.getInfrastructureMappingId());
      data.put(Constants.CLOUDPROVIDER_ID.getKey(), instance.getConnectorId());
      data.put(Constants.INSTANCE_TYPE.getKey(), instance.getInstanceType().name());
      data.put(Constants.ARTIFACT_ID.getKey(), instance.getLastArtifactId());
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
